pipeline {
    agent { label 'backend' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        SERVICE_NAME = 'api-gateway'
    }

    stages {
        stage('Initialize') {
            steps {
                echo "Starting build for ${env.SERVICE_NAME}..."
                sh 'mvn -version'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Compiling ${env.SERVICE_NAME}..."
                sh 'mvn -B clean compile -DskipTests'
            }
        }

        stage('Unit Test') {
            steps {
                echo "Running JUnit tests for ${env.SERVICE_NAME}..."
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo "Packaging ${env.SERVICE_NAME} into a JAR..."
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying ${env.SERVICE_NAME} to Render..."
                // withCredentials([string(credentialsId: 'render-deploy-hook-api-gateway', variable: 'RENDER_DEPLOY_HOOK')]) {
                //     sh 'curl -X POST "$RENDER_DEPLOY_HOOK"'
                // }
            }
        }
    }

    post {
        success {
            echo "SUCCESS: ${env.SERVICE_NAME} build and tests passed."
        }
        failure {
            echo "FAILURE: ${env.SERVICE_NAME} build or tests failed. Check logs and JUnit reports."
        }
        always {
            echo "Cleaning up workspace..."
            cleanWs()
        }
    }
}
