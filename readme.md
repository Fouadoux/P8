🗺️ TourGuide - Optimisation et Performance
📌 Description

TourGuide est une application Spring Boot permettant aux utilisateurs de trouver des attractions touristiques proches, de bénéficier de réductions sur les hôtels et les spectacles, et de suivre leurs récompenses.

Suite à une croissance rapide du nombre d’utilisateurs, le projet a subi des optimisations majeures pour améliorer ses performances et sa scalabilité.
⚙️ Technologies

    Back-end : Java 11+, Spring Boot
    Services externes : gpsUtil, RewardsCentral
    CI/CD : GitHub Actions, Jenkins, GitLabs

🚀 Optimisations Majeures

    Amélioration de gpsUtil : Réduction du temps de récupération des emplacements pour 100 000 utilisateurs en <15 min.
    Optimisation de RewardsCentral : Accélération du calcul des récompenses pour 100 000 utilisateurs en <20 min.
    Correction des bugs : Recommandations d’attractions, correction des tests intermittents.
    Pipeline CI/CD : Automatisation du build, des tests et du déploiement.
    
# Technologies

> Java 17  
> Spring Boot 3.X  
> JUnit 5  

# How to have gpsUtil, rewardCentral and tripPricer dependencies available ?

> Run : 
- mvn install:install-file -Dfile=/libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=/libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=/libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar
