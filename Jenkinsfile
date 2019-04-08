pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh 'mvn -s /var/jenkins_home/settings.xml clean verify'
      }
    }
  }
}