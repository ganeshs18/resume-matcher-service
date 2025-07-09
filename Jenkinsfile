pipeline {
  agent any

environment {
  APP_DIR = "/opt/resume-matcher"
  PM2_HOME = "/etc/pm2"
  JAR_NAME = "resumeMatcherService-0.0.1-SNAPSHOT.jar"
  BACKUP_DIR = "/opt/resume-matcher/backups"
  PM2_HOME = "/var/lib/jenkins/.pm2"
  KEYSTORE_PASSWORD = credentials('KEYSTORE_PASSWORD')

  SPRING_DATASOURCE_URL                 = credentials('SPRING_DATASOURCE_URL')
      SPRING_DATASOURCE_USERNAME            = credentials('SPRING_DATASOURCE_USERNAME')
      SPRING_DATASOURCE_PASSWORD            = credentials('SPRING_DATASOURCE_PASSWORD')


      SPRING_AI_VERTEX_AI_PROJECT_ID        = credentials('SPRING_AI_VERTEX_AI_PROJECT_ID')
      SPRING_AI_VERTEX_AI_CREDENTIALS_URI   = credentials('SPRING_AI_VERTEX_AI_CREDENTIALS_URI')

      AWS_S3_BUCKET_NAME                    = credentials('AWS_S3_BUCKET_NAME')
      AWS_ACCESS_KEY                        = credentials('AWS_ACCESS_KEY')
      AWS_SECRET_KEY                        = credentials('AWS_SECRET_KEY')

      JWT_SECRET                            = credentials('JWT_SECRET')

      GOOGLE_CLIENT_ID                      = credentials('GOOGLE_CLIENT_ID')
      GOOGLE_CLIENT_SECRET                  = credentials('GOOGLE_CLIENT_SECRET')
}

  triggers {
    pollSCM('* * * * *')  // or use webhook for real-time trigger
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main', url: 'git@github.com:ganeshs18/resume-matcher-service.git',credentialsId: 'vm-ssh-key'
      }
    }

    stage('Build JAR') {
      steps {
        sh 'mvn clean package -DskipTests'
      }
    }

    stage('Backup Old JAR') {
      steps {
        sh '''

        if [ -f "$APP_DIR/$JAR_NAME" ]; then
          timestamp=$(date +%Y%m%d_%H%M%S)
          cp $APP_DIR/$JAR_NAME $BACKUP_DIR/${JAR_NAME}_${timestamp}
        fi
        '''
      }
    }

    stage('Deploy New JAR') {
      steps {
        sh '''
        cp target/*.jar $APP_DIR/$JAR_NAME
        '''
      }
    }

   stage('Restart PM2') {
     steps {
       sh '''
       export PM2_HOME=$PM2_HOME
       pm2 delete resume-matcher-ai || true
       pm2 start /opt/resume-matcher/run.sh --name resume-matcher-ai
       pm2 save
       '''
     }
   }


  }

  post {
    failure {
      echo "Pipeline failed. Check logs."
    }
    success {
      echo "✅ Spring Boot app deployed and running via PM2."
    }
  }
}
