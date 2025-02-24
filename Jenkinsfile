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

        stage('Install Missing Dependencies') {
            steps {
                script {
                    echo 'Installation des dépendances locales...'
                    bat '''
                    cmd /c mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\gpsutil.jar -DgroupId=com.gpsutil -DartifactId=gpsutil -Dversion=1.0 -Dpackaging=jar
                    cmd /c mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\RewardCentral.jar -DgroupId=com.rewardcentral -DartifactId=rewardcentral -Dversion=1.0 -Dpackaging=jar
                    cmd /c mvn install:install-file -Dfile=C:\\Users\\fouad\\Documents\\openclassroom\\P8\\TourGuide\\libs\\TripPricer.jar -DgroupId=com.trippricer -DartifactId=trippricer -Dversion=1.0 -Dpackaging=jar
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    echo 'Compilation du projet...'
                    bat 'cmd /c mvn clean install -U || exit 1'
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    echo 'Exécution des tests unitaires...'
                    bat 'cmd /c mvn test || exit 1'
                }
            }
        }

        stage('Deploy to Local') {
            steps {
                script {
                    echo 'Déploiement en local...'
                    
                    // Copier le fichier JAR généré
                    bat 'cmd /c copy target\\tourguide-0.0.1-SNAPSHOT.jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\'

                    // Lancer l'application localement
                    bat 'cmd /c java -jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\tourguide-0.0.1-SNAPSHOT.jar'
                }
            }
        }

        stage('Clean Up') {
            steps {
                script {
                    echo 'Nettoyage des fichiers temporaires...'
                    bat 'cmd /c del /Q target\\*.jar'
                }
            }
        }
    }

    post {
        always {
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

