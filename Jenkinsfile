pipeline {
  agent any

environment {
  APP_DIR = "/opt/resume-matcher"
  JAR_NAME = "resumeMatcherService-0.0.1-SNAPSHOT.jar"
  BACKUP_DIR = "/opt/resume-matcher/backups"
  PM2_HOME = "/var/lib/jenkins/.pm2"
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
