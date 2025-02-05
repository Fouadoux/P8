pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17' // Image avec Maven et JDK 17
        }
    }

    environment {
        MAVEN_OPTS = "-Dmaven.repo.local=.m2/repository"
    }

    stages {
        stage('Checkout Repository') {
            steps {
                script {
                    checkout scm
                }
            }
        }

        stage('Cache Maven Dependencies') {
            steps {
                sh 'mvn dependency:go-offline'
            }
        }

        stage('Download Local JARs') {
            steps {
                sh '''
                mkdir -p libs
                curl -L -o libs/gpsUtil.jar "https://raw.githubusercontent.com/Fouadoux/P8/dev/libs/gpsUtil.jar"
                curl -L -o libs/RewardCentral.jar "https://raw.githubusercontent.com/Fouadoux/P8/dev/libs/RewardCentral.jar"
                curl -L -o libs/TripPricer.jar "https://raw.githubusercontent.com/Fouadoux/P8/dev/libs/TripPricer.jar"
                '''
            }
        }

        stage('Install Local JARs') {
            steps {
                sh '''
                mvn install:install-file -Dfile=libs/gpsUtil.jar -DgroupId=com.gpsutil -DartifactId=gpsutil -Dversion=1.0 -Dpackaging=jar
                mvn install:install-file -Dfile=libs/RewardCentral.jar -DgroupId=com.rewardcentral -DartifactId=rewardcentral -Dversion=1.0 -Dpackaging=jar
                mvn install:install-file -Dfile=libs/TripPricer.jar -DgroupId=com.trippricer -DartifactId=trippricer -Dversion=1.0 -Dpackaging=jar
                '''
            }
        }

        stage('Build with Maven') {
            steps {
                sh 'mvn -B package'
            }
        }

        stage('Run Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Archive Build Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy to Production') {
            when {
                expression { env.BRANCH_NAME == 'main' } // Déploiement seulement sur 'main'
            }
            steps {
                sh '''
                echo "Deploying to production..."
                # Ajoutez ici les commandes spécifiques au déploiement (ex: SCP, SSH, Docker, K8s)
                '''
            }
        }
    }
}
