# iFogSim2
A Toolkit for Modeling and Simulation of Resource Management Techniques in Internet of Things, Edge and Fog Computing Environments withe the following new features:
 * Mobility-support and Migration Management
   * Supporting real mobility datasets
   * Implementing different random mobility models 
 * Microservice Orchestration
 * Dynamic Distributed Clustering
 * Any Combinations of Above-mentioned Features 

## How to run iFogSim2 ?
* Eclipse IDE:
  * Create a Java project. 
  * Inside the project directory, initialize an empty Git repository with the following command
  ```
  git init
  ```
  * Add the Git repository of iFogSim2 as the `origin` remote.
  ```
  git remote add origin https://github.com/Cloudslab/iFogSim2
  ```
  * Pull the contents of the repository to your machine.
  ```
  git pull origin main
  ```
  * Include the JARs to your project.  
  * Run the example files (e.g. TranslationServiceFog_Clustering.java, CrowdSensing_Microservices_RandomMobility_Clustering.java) to get started.

* IntelliJ IDEA
  * Clone the iFogSim2 Git repository to desired folder:
  '''
  git clone https://github.com/Cloudslab/iFogSim2
  '''
  * Select "project from existing resources" from the "File" drop-down menu
  * Verify the Java version
  * Verify the external libraries in the "JARs" Folder are added to the project
  * Run the example files (e.g. TranslationServiceFog_Clustering.java, CrowdSensing_Microservices_RandomMobility_Clustering.java) to get started.

# References
 * Redowan Mahmud, Samodha Pallewatta , Mohammad Goudarzi, and Rajkumar Buyya, <A href="https://arxiv.org/abs/2109.05636">iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments</A>, September 2021.

