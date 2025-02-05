pipeline {
    agent any

    environment {
        JAVA_HOME = tool name: 'JDK 17', type: 'JDK'
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
                sh 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                // Exécuter les tests unitaires
                sh 'mvn test'
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
                    bat 'java -jar C:\\Users\\fouad\\Documents\\openclassroom\\P8\\deployTest\\your-app.jar'
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
