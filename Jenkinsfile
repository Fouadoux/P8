pipeline {
    agent any

    environment {
        JAVA_HOME = "C:\\Program Files\\Java\\jdk-22"
        PATH = "${JAVA_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Récupérer le code depuis Git
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Compiler avec Maven
                bat 'mvn clean install || exit 1'
            }
        }

        stage('Test') {
            steps {
                // Exécuter les tests unitaires
                bat 'mvn test || exit 1'
            }
        }

        stage('Deploy to Local') {
            steps {
                script {
                    // Simuler un déploiement local
                    echo 'Déploiement en local...'

                    // Vérifier si le fichier JAR existe
                    def jarFile = 'target\\tourguide-0.0.1-SNAPSHOT.jar' // Remplacez par votre nom de JAR
                    if (fileExists(jarFile)) {
                        // Copier le JAR généré vers le répertoire local de déploiement
                        bat "copy ${jarFile} C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\"
                        
                        // Simuler l'exécution de l'application localement
                        bat "java -jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\tourguide-0.0.1-SNAPSHOT.jar"
                    } else {
                        echo "Le fichier JAR n'a pas été trouvé dans le répertoire target."
                    }
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
