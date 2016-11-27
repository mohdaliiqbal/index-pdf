# index-pdf

The code uses Grobit to Parse PDF research papers and then converts the XML to JSON and pushes it into Elasticsearch. Once the data is in elastic search you should be able to search paper text and do some priliminary analysis very easily. You may use Kibana to build dashboard around various statistics like frequent terms in papers etc.
To build the project use the following command

    mvn clean install

You must run this from within IDE as I have not configured/tested it any other way.
