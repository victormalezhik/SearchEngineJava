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

## Data Base structure

* **Site** - information about sites and their indexing statuses

* **Page** — indexed site pages

* **Lemma** - lemmas found in texts

* **Index** — search index

## API Specification

* Starting full indexing - GET/api/startIndexing

The method starts a full indexing of all sites or a complete re-indexing if they are already indexed.
If indexing or reindexing is currently running, the method returns an appropriate error message.

* Stop current indexing - GET/api/stopIndexing

The method stops the current indexing (re-indexing) process. If indexing or reindexing is not currently occurring, the method returns an appropriate error message.

* Adding or updating a single page - POST/api/indexPage

The method adds to the index or updates a separate page, the address of which is passed in the parameter.
If the page address is passed incorrectly, the method should return the appropriate error.

* Adding or updating a single page - POST/api/indexPage

The method adds to the index or updates a separate page, the address of which is passed in the parameter.
If the page address is passed incorrectly, the method should return the appropriate error.

*Retrieving data from a search query - GET /api/search

The method searches for pages using the passed search query (query parameter).
To display results in portions, you can also set the parameters offset (offset from the beginning of the list of results) and limit (the number of results that need to be displayed).
The response displays the total number of results (count), which does not depend on the values of the offset and limit parameters, and a data array with the search results. Each result is an object containing the properties of the search result (see below for the structure and description of each property).
If the search query is not specified or there is no ready index yet (the site we are searching for, or all sites are not indexed at once), the method should return the appropriate error (see example below). Error texts must be clear and reflect the essence of the errors.

## Сontacts
E-mail: victormalezhik@gmail.com

## Аuthor

Viktor Malezhyk
