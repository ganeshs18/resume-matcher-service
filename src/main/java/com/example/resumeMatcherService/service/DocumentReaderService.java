package com.example.resumeMatcherService.service;

import com.example.resumeMatcherService.dto.ResumeEvent;
import com.example.resumeMatcherService.entity.ResumeBlockEntity;
import com.example.resumeMatcherService.entity.ResumeEntity;
import com.example.resumeMatcherService.entity.UserEntity;
import com.example.resumeMatcherService.repository.ResumeBlockRepository;
import com.example.resumeMatcherService.repository.ResumeRepository;
import com.example.resumeMatcherService.repository.UserRepository;
import org.apache.poi.xwpf.usermodel.*;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
public class DocumentReaderService {


    @Autowired
    private ResumeRepository resumeRepo;
    @Autowired
    private ResumeBlockRepository blockRepo;
    @Autowired
    private VertexAiService vertexAiService;
    @Autowired
    private S3StorageService s3Service;
    @Autowired
    private UserRepository userRepo;


    public String extractContentFromMultipart(MultipartFile file) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        return readContent(resource);
    }


    public String extractContentFromS3(String s3Url) throws IOException {
        return readContent(new UrlResource(s3Url));
    }

    private String readContent(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> docs = reader.get();
        return docs.isEmpty() ? "" : docs.get(0).getFormattedContent();
    }


    public Flux<ResumeEvent> enhanceResumeStream(
            String s3Key,
            String jobTitle,
            String company,
            Long userId,
            String rawJobDescription) {

        return Flux.concat(
                // Immediate UPLOAD_START event (non-blocking)
                Flux.just(new ResumeEvent(ResumeEvent.EventType.UPLOAD_START, "Uploading file to S3...", null, 5)),

                // Get file from S3 (blocking wrapped in defer)
                Mono.defer(() -> Mono.fromCallable(() -> s3Service.getFile(s3Key))
                                .subscribeOn(Schedulers.boundedElastic()))
                        .flatMapMany(file -> {

                            // Get user from DB (blocking wrapped)
                            return Mono.defer(() -> Mono.fromCallable(() -> {
                                        Optional<UserEntity> userOpt = userRepo.findById(String.valueOf(userId));
                                        return userOpt.orElseThrow(() -> new RuntimeException("Invalid userId: " + userId));
                                    }).subscribeOn(Schedulers.boundedElastic()))

                                    .flatMapMany(user -> {

                                        // Save ResumeEntity
                                        return Mono.defer(() -> Mono.fromCallable(() -> {
                                                    ResumeEntity resume = new ResumeEntity();
                                                    resume.setFileName(file.getOriginalFilename());
                                                    resume.setOldS3Url(s3Key);  // Already uploaded
                                                    resume.setOwner(user);
                                                    resume.setUploadedAt(LocalDateTime.now());
                                                    return resumeRepo.save(resume);
                                                }).subscribeOn(Schedulers.boundedElastic()))

                                                .flatMapMany(resume -> {

                                                    // Extract blocks
                                                    return Mono.defer(() -> Mono.fromCallable(() -> extractBlocks(file, resume))
                                                                    .subscribeOn(Schedulers.boundedElastic()))
                                                            .flatMapMany(blocks -> {

                                                                if (blocks.isEmpty()) {
                                                                    return Flux.just(new ResumeEvent(ResumeEvent.EventType.ERROR, "No blocks found.", null, 0));
                                                                }

                                                                ResumeEvent parsingEvent = new ResumeEvent(
                                                                        ResumeEvent.EventType.PARSING_BLOCKS,
                                                                        blocks.size() + " blocks parsed.",
                                                                        null,
                                                                        20
                                                                );

                                                                Map<String, ResumeBlockEntity> blockMap = blocks.stream()
                                                                        .collect(Collectors.toMap(
                                                                                block -> "block-" + block.getBlockIndex(),
                                                                                Function.identity()
                                                                        ));

                                                                Map<String, String> blockTexts = blockMap.entrySet().stream()
                                                                        .collect(Collectors.toMap(
                                                                                Map.Entry::getKey,
                                                                                entry -> entry.getValue().getOriginalText()
                                                                        ));

                                                                AtomicInteger counter = new AtomicInteger(0);
                                                                int total = blockTexts.size();

                                                                // Call Gemini streaming and emit AI_RAW_STREAM
                                                                Flux<ResumeEvent> aiStream = vertexAiService.streamRawEnhancedResume(
                                                                                blockTexts, jobTitle, company, rawJobDescription
                                                                        )
                                                                        .map(entry -> {
                                                                            int progress = 50 + (counter.incrementAndGet() * 50 / total);
                                                                            return new ResumeEvent(
                                                                                    ResumeEvent.EventType.AI_RAW_STREAM,
                                                                                    entry.getMessage(),
                                                                                    null,
                                                                                    progress
                                                                            );
                                                                        })
                                                                        .onErrorResume(e -> Flux.just(new ResumeEvent(
                                                                                ResumeEvent.EventType.ERROR,
                                                                                "Vertex AI streaming error: " + e.getMessage(),
                                                                                null,
                                                                                0
                                                                        )));

                                                                return Flux.concat(
                                                                        Flux.just(
                                                                                new ResumeEvent(ResumeEvent.EventType.UPLOAD_SUCCESS, "File uploaded and resume created.", null, 10),
                                                                                parsingEvent
                                                                        ),
                                                                        aiStream,
                                                                        Flux.just(new ResumeEvent(ResumeEvent.EventType.COMPLETE, (String.valueOf(resume.getId())), null, 100))
                                                                );
                                                            });
                                                });
                                    });
                        })
                        .onErrorResume(e -> Flux.just(new ResumeEvent(
                                ResumeEvent.EventType.ERROR,
                                "Unexpected error: " + e.getMessage(),
                                null,
                                0
                        )))
        );
    }




    public List<ResumeBlockEntity> extractBlocks(MultipartFile file, ResumeEntity resume) throws IOException {
        List<ResumeBlockEntity> blocks = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {

            blocks.addAll(extractDocxToEntities(inputStream, resume));


            return blockRepo.saveAll(blocks);
        } catch (Exception e) {
            throw new IOException("Failed to extract document content: " + e.getMessage(), e);
        }
    }




    private List<ResumeBlockEntity> extractDocxToEntities(InputStream docxInputStream, ResumeEntity resume) throws IOException {
        XWPFDocument document = new XWPFDocument(docxInputStream);
        List<ResumeBlockEntity> blocks = new ArrayList<>();

        List<IBodyElement> bodyElements = document.getBodyElements();
        int blockIndex = 0;

        for (IBodyElement element : bodyElements) {
            ResumeBlockEntity block = new ResumeBlockEntity();
            block.setResume(resume);
            block.setBlockIndex(blockIndex++);
            block.setCreatedAt(LocalDateTime.now());

            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText().trim();

                String blockType = "paragraph";
                String style = paragraph.getStyle();

                // Detect header
                if (style != null && style.toLowerCase().contains("heading")) {
                    blockType = "header";
                }
                // Detect horizontal rule
                else if (text.isEmpty() && hasHorizontalLine(paragraph)) {
                    blockType = "hr";
                }
                // Detect list item
                else if (paragraph.getNumID() != null) {
                    XWPFNumbering numbering = document.getNumbering();
                    BigInteger numId = paragraph.getNumID();
                    BigInteger ilvl = paragraph.getNumIlvl();

                    if (numbering != null) {
                        XWPFNum num = numbering.getNum(numId);
                        if (num != null) {
                            CTNum ctNum = num.getCTNum();
                            BigInteger abstractNumId = ctNum.getAbstractNumId().getVal();
                            XWPFAbstractNum absNum = numbering.getAbstractNum(abstractNumId);
                            if (absNum != null) {
                                CTAbstractNum ctAbsNum = absNum.getCTAbstractNum();
                                if (ctAbsNum.sizeOfLvlArray() > ilvl.intValue()) {
                                    CTLvl lvl = ctAbsNum.getLvlArray(ilvl.intValue());
                                    STNumberFormat.Enum fmt = lvl.getNumFmt().getVal();
                                    if (fmt != null) {
                                        if ("bullet".equalsIgnoreCase(fmt.toString())) {
                                            blockType = "bullet";
                                        } else {
                                            blockType = "numbered";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                XWPFRun run = paragraph.getRuns().isEmpty() ? null : paragraph.getRuns().get(0);

                block.setOriginalText(text);
                block.setEnhancedText(null);
                block.setFont(run != null && run.getFontFamily() != null ? run.getFontFamily() : "Times New Roman");
                block.setFontSize(run != null && run.getFontSize() > 0 ? run.getFontSize() : 12);
                block.setBold(run != null && run.isBold());
                block.setItalic(run != null && run.isItalic());
                block.setUnderline(run != null && !UnderlinePatterns.NONE.equals(run.getUnderline()));
                block.setAlignment(paragraph.getAlignment() != null ? paragraph.getAlignment().name().toLowerCase() : "left");
                block.setSpacing((float) paragraph.getSpacingBetween());
                block.setBlockType(blockType);
            }

            else if (element instanceof XWPFTable table) {
                StringBuilder tableText = new StringBuilder();
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        tableText.append(cell.getText()).append(" | ");
                    }
                    tableText.append("\n");
                }

                block.setOriginalText(tableText.toString().trim());
                block.setEnhancedText(null);
                block.setFont("Times New Roman");
                block.setFontSize(12);
                block.setBold(false);
                block.setItalic(false);
                block.setUnderline(false);
                block.setAlignment("left");
                block.setSpacing(1.15f);
                block.setBlockType("table");
            }

            blocks.add(block);
        }

        return blocks;
    }


    private boolean hasHorizontalLine(XWPFParagraph paragraph) {
        return paragraph.getBorderBottom() != null && !paragraph.getBorderBottom().equals(Borders.NONE);
    }

    public void writeEntitiesToDocx(List<ResumeBlockEntity> blocks, OutputStream outputStream) throws IOException {
        XWPFDocument document = new XWPFDocument();

        for (ResumeBlockEntity block : blocks) {
            String type = block.getBlockType();

            if ("paragraph".equalsIgnoreCase(type) || "header".equalsIgnoreCase(type)) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.valueOf(block.getAlignment().toUpperCase()));
                paragraph.setSpacingBetween(block.getSpacing());

                if ("header".equalsIgnoreCase(type)) {
                    paragraph.setStyle("Heading1"); // Optional, depending on need
                }

                XWPFRun run = paragraph.createRun();
                run.setText(block.getEnhancedText() != null ? block.getEnhancedText() : block.getOriginalText());
                run.setFontFamily(block.getFont());
                run.setFontSize(block.getFontSize());
                run.setBold(block.isBold());
                run.setItalic(block.isItalic());
                run.setUnderline(block.isUnderline() ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);

            } else if ("hr".equalsIgnoreCase(type)) {
                XWPFParagraph hrPara = document.createParagraph();
                hrPara.setBorderBottom(Borders.SINGLE);
            } else if ("table".equalsIgnoreCase(type)) {
                XWPFTable table = document.createTable();

                String[] rows = block.getOriginalText().split("\n");
                for (int i = 0; i < rows.length; i++) {
                    String[] cells = rows[i].split("\\|");
                    XWPFTableRow row = i == 0 ? table.getRow(0) : table.createRow();
                    for (int j = 0; j < cells.length; j++) {
                        if (i != 0 && j >= row.getTableCells().size()) {
                            row.addNewTableCell();
                        }
                        row.getCell(j).setText(cells[j].trim());
                    }
                }
            }
        }

        document.write(outputStream);
        outputStream.close();
    }



}
