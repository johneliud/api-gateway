pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        SERVICE_NAME = 'api-gateway'
        DOCKER_REGISTRY = credentials('docker-registry')
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

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image for ${env.SERVICE_NAME}..."
                sh "docker build -t ${env.SERVICE_NAME}:${env.BUILD_NUMBER} ."
                sh "docker tag ${env.SERVICE_NAME}:${env.BUILD_NUMBER} ${env.SERVICE_NAME}:latest"
            }
        }

        stage('Push to Registry') {
            when {
                branch 'main'
            }
            steps {
                echo "Pushing Docker image to registry..."
                sh "docker push ${env.DOCKER_REGISTRY}/${env.SERVICE_NAME}:${env.BUILD_NUMBER}"
                sh "docker push ${env.DOCKER_REGISTRY}/${env.SERVICE_NAME}:latest"
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
