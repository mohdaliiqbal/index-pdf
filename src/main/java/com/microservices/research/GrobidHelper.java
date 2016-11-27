package com.microservices.research;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.grobid.service.GrobidPathes;
import org.tei_c.ns._1.*;
import org.tei_c.ns._1.Date;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alim20 on 8/11/16.
 */
public class GrobidHelper {


    private static String host = "http://192.168.99.100:8080";
    static Client create = Client.create();
    public static void main(String[] args) throws Exception {
        processPaper("/Users/alim20/Google Drive/nuha-phd/Papers/papers selected in SLR/5.pdf");
    }

    public static Paper processPaper(String filePath) throws Exception {
        File pdfFile = new File(filePath);

        WebResource service = create.resource(getHost());

        ClientResponse response;

        FormDataMultiPart form = new FormDataMultiPart();
        form.field("input", pdfFile, MediaType.MULTIPART_FORM_DATA_TYPE);
        form.field("consolidate", "0", MediaType.MULTIPART_FORM_DATA_TYPE);

        service = Client.create().resource(
                getHost() + GrobidPathes.PATH_GROBID + "/"
                        + GrobidPathes.PATH_FULL_TEXT);
        response = service.type(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_XML)
                .post(ClientResponse.class, form);

        InputStream inputStream = response.getEntity(InputStream.class);
//        String tei = TextUtilities.convertStreamToString(inputStream);

        JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        TEI tei = (TEI) jaxbUnmarshaller.unmarshal(inputStream);

        String fileName = pdfFile.getName();
        String id = fileName.substring(0, fileName.indexOf('.'));

        //System.out.println(json);

        Paper paper = new Paper();
        paper.setId(id);

        //title
        StringBuilder title = new StringBuilder();

        tei.getTeiHeader().getFileDesc().getTitleStmt()
                .getTitles()
                .forEach( x -> title.append(" "+recursiveContentExtract( x.getContent())) );

        paper.title = title.toString().trim();

        tei.getTeiHeader().getFileDesc().getSourceDescs()
                .stream()
                .map(x -> x.getBiblsAndBiblStructsAndListBibls())
                .flatMap(x -> x.stream())
                .filter(p -> p instanceof BiblStruct && ((BiblStruct)p).getAnalytics().size() > 0 )
                .map( p -> processBibliographies((BiblStruct)p))
                .filter( p -> p != null)
                .forEach(p-> paper.authors = p.authors);


        //abstract
        StringBuilder abstracts = new StringBuilder();
        List<String> keywords = new ArrayList<String>();

        tei.getTeiHeader().getProfileDescsAndXenoDatas().stream()
                .map( x-> {
                    if( x instanceof ProfileDesc )
                        return ((ProfileDesc)x).getAbstractsAndTextClassesAndCorrespDescs();
                    else
                        return x;
                })
                .forEach(x -> {
                    if( x instanceof String && ((String) x).trim().length()>0 )
                        abstracts.append(" "+x);
                    else if( x instanceof  Abstract )
                        abstracts.append( recursiveContentExtract( ((Abstract) x).getContent()));
                    else if( x instanceof List){
                        ((List<Object>) x).stream()
                                .forEach( y-> {
                                    if( y instanceof Abstract){
                                        abstracts.append( recursiveContentExtract( ((Abstract) y).getContent()));
                                    }else if( y instanceof TextClass ){
                                        ((TextClass)y).getClassCodesAndKeywords().stream()
                                                .forEach( k -> {
                                                    if( k instanceof Keywords ){
                                                        List<Object> content = ((Keywords) k).getContent();
                                                        content.stream()
                                                                .forEach( c -> {
                                                                    if( c instanceof TeiMacroPhraseSeq )
                                                                        keywords.add(recursiveContentExtract(((TeiMacroPhraseSeq)c).getContent()));
                                                                    else if( c instanceof org.tei_c.ns._1.List){
                                                                        System.out.println(c);
                                                                    } else if( c instanceof String && ((String) c).trim().length()>0 )
                                                                        keywords.add((String) c);
                                                                });
                                                        ;
                                                    }else if( k instanceof ClassCode ){
                                                        System.out.println(((ClassCode)k).getContent());
                                                    }
                                                });
                                    }
                                });
                    }


                });

        paper.abstracts = abstracts.toString().trim();
        paper.keywords = keywords;

        //body
        tei.getText().getLinksAndAnchorsAndNotes()
                .stream()
                .filter(p -> p instanceof Body)
                .map(p -> ((Body) p).getLinksAndAnchorsAndNotes())
                .forEach(p-> processBodyElements(p, paper));

        List<Bibliography> bibliographyList = new ArrayList<Bibliography>();
        //Process bibliographic data
        tei.getText()
                .getLinksAndAnchorsAndNotes()
                .stream()
                .filter( p -> p instanceof Back)
                .map( p -> ((Back) p).getHeadsAndPSAndTrashes() )
                .flatMap(l -> l.stream())
                .collect(Collectors.toList())
                .stream()
                .filter( p -> p instanceof Div)
                .map( p -> ((Div) p).getMeetingsAndHeadsAndLinks())
                .flatMap( l -> l.stream())
                .collect(Collectors.toList())
                .stream()
                .filter(p -> p instanceof ListBibl)
                .map( p -> ((ListBibl) p).getBiblsAndBiblStructsAndListBibls())
                .flatMap( l -> l.stream())
                .collect(Collectors.toList())
                .stream()
                .filter(p -> p instanceof BiblStruct && ((BiblStruct)p).getAnalytics().size() > 0 )
                .map( p -> processBibliographies((BiblStruct)p))
                .filter( p -> p != null)
                .forEach(p-> bibliographyList.add(p));

        paper.bibliographies = bibliographyList;
               /* .map(p -> ((BiblStruct) p).getMonogrsAndSeries())
                .flatMap( l -> l.stream())
                .collect(Collectors.toList())
                .stream()
                .filter(p -> p instanceof Monogr)
                .map( p -> ((Monogr) p).getImprintsAndAuthorsAndEditors())

                .flatMap( l -> l.stream())
                .collect(Collectors.toList())
                .stream()
                .filter( p -> p instanceof  Title )
                .map( p -> (Title) p)
                .forEach(b -> System.out.println(b.getContent()));*/

        //System.out.println("Tei:"+tei);
        return paper;

    }

    private static void processHeaderElements(List<Object> p, Paper paper) {

    }

    private static void processBodyElements(List<Object> p, Paper paper) {

        StringBuilder content = new StringBuilder();

        for(Object x: p){
            if( x instanceof Div ){
               content.append( " "+recursiveContentExtract( ((Div) x).getMeetingsAndHeadsAndLinks() ));
            }
            else if( x instanceof Figure ){
                content.append( " "+recursiveContentExtract( ((Figure) x).getHeadsAndPSAndTrashes() ));
            }

        }

        paper.content = content.toString();
    }

    private static String recursiveContentExtract(List<Object> objects)  {

        StringBuilder builder = new StringBuilder();
        objects.stream()
                .map(x -> {

                    if( x instanceof String )
                        return x;

                    try {
                        Method getContent = x.getClass().getMethod("getContent", null);
                        if ( getContent != null){
                            Object child =  getContent.invoke(x);
                            if( child instanceof String)
                                return child;
                            else
                                return recursiveContentExtract((List<Object>)child);
                        }


                    } catch (NoSuchMethodException e) {

                        //now use reflection to find method with List<Object> type and do recursion on that.
                        for( Method m : x.getClass().getMethods() ) {
                            if( List.class.isAssignableFrom(m.getReturnType())){
                                Object child = null;
                                try {
                                    child = m.invoke(x);
                                    return recursiveContentExtract((List<Object>)child);
                                } catch (IllegalAccessException e1) {
                                    e1.printStackTrace();
                                } catch (InvocationTargetException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }


                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return "";
                })
                .forEach(x-> builder.append(" "+x));

        return builder.toString();
    }

    private static String processFigureElements(Object d) {
        if( d instanceof  FigDesc) {
            System.out.println(((FigDesc) d).getContent());
        } else if( d instanceof  Head) {
            System.out.println(((Head) d).getContent());
        } else
            System.out.println(d);
        return "";
    }

    private static String processDivElements(Object d) {
        if( d instanceof  Head) {
            System.out.println(((Head) d).getContent());
        } else if( d instanceof  P) {
            System.out.println(((P) d).getContent());
        } else
            System.out.println(d);
        return "";
    }

    private static Bibliography processBibliographies(BiblStruct biblStruct) {
        ArrayList<PaperAuthor> authors = new ArrayList<PaperAuthor>();
        StringBuilder title = new StringBuilder();
        StringBuilder monTitle = new StringBuilder();
        StringBuilder meeting = new StringBuilder();
        StringBuilder imprint = new StringBuilder();

        List<String> editors = new ArrayList<String>();

        biblStruct.getAnalytics()
                .stream()
                .map(p -> p.getAuthorsAndEditorsAndTitles())
                .flatMap( o -> o.stream())
                .collect( Collectors.toList())
                .stream()
                .filter(p -> p instanceof Author  && ((Author) p).getContent().size() > 0)
                .map(  p -> processAuthors( ((Author) p).getContent() ))
                .forEach(p->authors.add(p));


        biblStruct.getAnalytics()
                .stream()
                .map(p -> p.getAuthorsAndEditorsAndTitles() )
                .flatMap( o -> o.stream())
                .collect( Collectors.toList())
                .stream()
                .filter(p -> p instanceof Title  && ((Title) p).getContent().size() > 0)
                .map( p -> ((Title) p).getContent() )
                .flatMap( o -> o.stream() )
                .collect( Collectors.toList())
                .stream()
                .forEach(p -> title.append(p));

        biblStruct.getMonogrsAndSeries()
                .stream()
                .filter(m -> m instanceof Monogr && ((Monogr) m).getImprintsAndAuthorsAndEditors().size() > 0 )
                .map(m -> ((Monogr) m).getImprintsAndAuthorsAndEditors() )
                .flatMap( o -> o.stream() )
                .forEach( x-> processMonogr(x, monTitle,meeting,imprint, editors));



        if( authors.size() == 0 && title.length() == 0)
            return null;

        return new Bibliography(authors, title.toString().trim(),
                monTitle.toString().trim(), meeting.toString().trim(),
                imprint.toString().trim(), editors);
    }

    private static void processMonogr(Object x, StringBuilder monTitle, StringBuilder meeting, StringBuilder imprint, List<String> editor) {
         if(x instanceof Title)
             processTitle(monTitle, (Title) x);
         else if (x instanceof Meeting)
            processMeeting(meeting,(Meeting) x);
         else if (x instanceof Imprint)
             processImprint( imprint, (Imprint) x );
          else if( x instanceof Editor)
             editor.add(" "+recursiveContentExtract(((Editor) x).getContent()));
        else
             System.out.println(x);

    }

    private static void processTitle(StringBuilder monTitle, Title x) {
         monTitle.append(" "+recursiveContentExtract(x.getContent()));
    }

    private static void processMeeting(StringBuilder meeting, Meeting x) {
        meeting.append(" "+recursiveContentExtract(x.getContent()));
    }

    private static void processImprint(StringBuilder imprint, Imprint x) {
        List<Object> biblScopesAndDatesAndPubPlaces = x.getBiblScopesAndDatesAndPubPlaces();
        for (Object object: biblScopesAndDatesAndPubPlaces
             ) {
            if( object instanceof BiblScope ){

                String s = (((BiblScope) object).getFrom() != null) ? ((BiblScope) object).getFrom() + "-" + ((BiblScope) object).getTo() : "";
                imprint.append(" "+((BiblScope) object).getUnit()+ " "+s+ " "+ recursiveContentExtract(((BiblScope) object).getContent()) );
            } else if( object instanceof Date){
                imprint.append(" "+((Date) object).getWhen());
            }

        }
    }

    private static PaperAuthor processAuthors(List<Object> content) {

        StringBuilder builder = new StringBuilder();
        PaperAuthor author = new PaperAuthor();

        content.stream()
                .forEach( p-> updateAuthor(author, p) );



        return author;
    }

    private static void updateAuthor(PaperAuthor author, Object p) {
        if( p instanceof  PersName){
            ((PersName) p).getContent()
                    .stream()
                    .forEach( x-> {
                        if( x instanceof Forename )
                            author.firstName = (String) ((Forename) x).getContent().get(0);
                        else if( x instanceof Surname)
                            author.lastName = (String) ((Surname)x).getContent().get(0);
                    });

        }
        else if( p instanceof Affiliation ){
            ((Affiliation) p).getContent().stream()
                    .forEach( x -> {
                        if( x instanceof OrgName )
                            author.affiliation = (String) ((OrgName)x).getContent().get(0);
                    });
        }
        else if( p instanceof String && p.toString().trim().length()>0)
            System.out.println("Wht:"+p);
    }

    private static String fillBuilderWithArray( Object p) {

        StringBuilder builder = new StringBuilder();

        if( p instanceof ArrayList)
        {
            ((ArrayList) p).forEach(x -> builder.append(x+" "));
        }

        return builder.toString();
    }

    public static String getHost() {
        return host;
    }
}
