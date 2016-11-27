package com.microservices.research;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.apache.http.HttpResponse;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.apache.http.client.HttpClient;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Main {

    public static final String ELASTICSEARCH_HOST = "192.168.99.100";
    public static final int ELASTICSEARCH_PORT = 9300;

    //following PATH_TO_PDF_PAPERS points to the directory where papers are located.
    static String PATH_TO_PDF_PAPERS = "/Users/alim20/Google Drive/nuha-phd/Papers/papers selected in SLR";


    static Client client = null;
    static TransportClient transportClient = null;

    public static void main(String[] args) throws UnknownHostException {


        if( args.length > 0 )
            PATH_TO_PDF_PAPERS = args[0];

        client = TransportClient.builder().build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ELASTICSEARCH_HOST), ELASTICSEARCH_PORT));

        File file = new File(PATH_TO_PDF_PAPERS);
        if( file.exists() ){
            Arrays.stream(file.list())
                    .filter(x-> {
                        File file1 = new File(PATH_TO_PDF_PAPERS + "/" + x);
                        return file1.exists() && file1.isFile() && file1.getName().endsWith("pdf");
                    } )
                    .map(x -> {
                        try {
                            return GrobidHelper.processPaper(PATH_TO_PDF_PAPERS + "/" + x);
                        } catch (Exception e) {
                            try {
                               return attemptTikaParsing( PATH_TO_PDF_PAPERS +"/"+x);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (TikaException e1) {
                                e1.printStackTrace();
                            } catch (SAXException e1) {
                                e1.printStackTrace();
                            }

                        }
                        return null;
                    })
            .filter( p -> p != null)
            .map( p -> {
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                try {
                    return ow.writeValueAsString(p);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }).forEach(Main::submitToElasticsearch);
        }

        client.close();
    }

    private static Paper attemptTikaParsing(String path) throws IOException, TikaException, SAXException {

        File file = new File(path);
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));

        Parser parser = new AutoDetectParser();

        ContentHandler handler = new BodyContentHandler();

        Metadata metadata = new Metadata();

        parser.parse(is, handler, metadata, new ParseContext());

        Paper paper = new Paper();
        String fileName = file.getName();
        String id = fileName.substring(0, fileName.indexOf('.'));
        paper.setId(id);
        paper.setContent(handler.toString());
        return paper;
    }


    public static void submitToElasticsearch(String json){

        //System.out.println("JSON:"+json);

            try {


                IndexResponse response = client.prepareIndex("research", "paper")
                        .setSource(json
                        )
                        .get();


                System.out.println("response:" + response.getId());


            } catch (ElasticsearchException e) {
                e.printStackTrace();
                //
            }

    }

    public void executeMultiPartRequest(String urlString, File file, String fileName, String fileDescription) throws Exception
    {
        HttpClient client = new DefaultHttpClient() ;
        HttpPost postRequest = new HttpPost (urlString) ;
        try
        {
            //Set various attributes
            MultipartEntity multiPartEntity = new MultipartEntity () ;
            multiPartEntity.addPart("fileDescription", new StringBody(fileDescription != null ? fileDescription : "")) ;
            multiPartEntity.addPart("fileName", new StringBody(fileName != null ? fileName : file.getName())) ;

            FileBody fileBody = new FileBody(file, "application/octect-stream") ;
            //Prepare payload
            multiPartEntity.addPart("attachment", fileBody) ;

            //Set to request body
            postRequest.setEntity(multiPartEntity) ;

            //Send request
           HttpResponse response = client.execute(postRequest) ;

            //Verify response if any
            if (response != null)
            {
                System.out.println(response.getStatusLine().getStatusCode());
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace() ;
        }
    }


}
