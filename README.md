# SearchEngineProect

The search engine should be a Spring application (JAR file that runs on any server or computer), working with a locally installed MySQL database, having a simple web interface and API through which it can be controlled and receive search results upon request.

## Start of the work

You can clone the project by pasting this link into your IDE

https://github.com/victormalezhik/MyFinalTrialProject.git

## Description 
The web interface (frontend component) of the project is one web page with three tabs:

* **Dashboard**
This tab opens by default. It displays general statistics for all sites, as well as detailed statistics and status for each site (statistics obtained by requesting /api/statistics).

* **Management**
This tab contains search engine management tools - starting and stopping full indexing (re-indexing), as well as the ability to add (update) a separate page using a link.

* **Search**
This page is intended for testing the search engine. It contains a search field, a drop-down list with the choice of a site to search, and when you click on the “Find” button, the search results are displayed (using the API request /api/search).
All information on the tabs is loaded by requests to the API of your application. When buttons are clicked, requests are also sent.


## Сontacts
E-mail: victormalezhik@gmail.com

## Аuthor

Viktor Malezhyk
