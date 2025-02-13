pipeline {
    agent any

    environment {
        JAVA_HOME = "C:\\Program Files\\Java\\jdk-22"
        PATH = "${JAVA_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Checkout') {

            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Deploy to Local') {
            steps {
                checkout scm
            }
        }

        stage('Install Missing Dependencies') {
            steps {
                script {
                    bat '''
                    mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\gpsutil.jar -DgroupId=com.gpsutil -DartifactId=gpsutil -Dversion=1.0 -Dpackaging=jar
                    mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\RewardCentral.jar -DgroupId=com.rewardcentral -DartifactId=rewardcentral -Dversion=1.0 -Dpackaging=jar
                    mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\TripPricer.jar -DgroupId=com.trippricer -DartifactId=trippricer -Dversion=1.0 -Dpackaging=jar
                    '''
                    echo 'Déploiement en local...'
                    
                    bat 'copy target\\*.jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\'
                    
                    bat 'java -jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\your-app.jar'
                }
            }
        }
    }

        stage('Build') {
            steps {
                // Compiler avec Maven
                bat 'mvn clean install -U || exit 1'
            }
        }

        stage('Test') {
            steps {
                // Exécuter les tests unitaires
                bat 'mvn test'
            }
        }

        stage('Deploy to Local') {
            steps {
                script {
                    // Simuler un déploiement local
                    echo 'Déploiement en local...'
                    
                    // Copier le JAR généré vers le répertoire local de déploiement
                    bat 'copy target\\*.jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\'
                    
                    // Simuler l'exécution de l'application localement
                    bat 'java -jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\tourguide-0.0.1-SNAPSHOT.jar'
                }
            }
        }
    }

    post {
        always {
            // Actions à effectuer après chaque pipeline, qu'il réussisse ou échoue
            echo 'Pipeline terminé.'
        }
        success {
            echo 'Le pipeline a réussi.'
        }
        failure {
            echo 'Le pipeline a échoué.'
        }
    }
}
