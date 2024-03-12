package jadelab1.homework;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;


public class ServiceAgent extends Agent {
    protected void setup() {
        //services registration at DF
        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());

        //service desription
        ServiceDescription sd = new ServiceDescription();
        sd.setType("answers");
        sd.setName("dictionary");


        dfad.addServices(sd);
        try {
            DFService.register(this, dfad);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }


        addBehaviour(new DictionaryCyclicBehaviour(this));
        //doDelete();
    }

    protected void takeDown() {
        //services deregistration before termination
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }
    }

    public String makeRequest(String serviceName, String word) {
        StringBuffer response = new StringBuffer();
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            DataInputStream input;
            url = new URL("http://dict.org/bin/Dict");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String content = "Form=Dict1&Strategy=*&Database=" + URLEncoder.encode(serviceName) + "&Query=" + URLEncoder.encode(word) + "&submit=Submit+query";
            //forth
            printout = new DataOutputStream(urlConn.getOutputStream());
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            //back
            input = new DataInputStream(urlConn.getInputStream());
            String str;
            while (null != ((str = input.readLine()))) {
                response.append(str);
            }
            input.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        //cut what is unnecessary
        return response.substring(response.indexOf("<hr>") + 4, response.lastIndexOf("<hr>"));
    }
}

/**
 * DictionaryCyclicBehaviour behaviour of agent.
 * Resolve requests for agent in format "database:word" or "word".
 * */
class DictionaryCyclicBehaviour extends CyclicBehaviour {
    protected ServiceAgent agent;
    //Databases present on site
    private Set<String> databases = new HashSet<>();

    public DictionaryCyclicBehaviour(ServiceAgent agent) {
        this.agent = agent;
        databases = Set.of("*", "!", "gcide", "wn", "moby-thesaurus", "elements",
                "vera", "jargon", "foldoc", "easton", "hitchcock", "bouvier", "devil", "world02", "gaz2k-counties",
                "gaz2k-places", "gaz2k-zips", "fd-hrv-eng", "fd-fin-por", "fd-fin-bul", "fd-fra-bul", "fd-deu-swe",
                "fd-fin-swe", "fd-jpn-rus", "fd-wol-fra", "fd-fra-pol", "fd-eng-deu", "fd-deu-nld", "fd-por-eng", "fd-spa-deu",
                "fd-ces-eng", "fd-swe-fin", "fd-eng-pol", "fd-pol-nor", "fd-eng-rom", "fd-eng-fra", "fd-fin-ell", "fd-eng-lit",
                "fd-ckb-kmr", "fd-ita-eng", "fd-pol-eng", "fd-gle-eng", "fd-eng-tur", "fd-gle-pol", "fd-pol-deu", "fd-fra-spa",
                "fd-lit-eng", "fd-eng-jpn", "fd-ara-eng", "fd-nld-ita", "fd-eng-lat", "fd-eng-hun", "fd-ita-jpn", "fd-dan-eng",
                "fd-hun-eng");
)
    }

    public void action() {
        final MessageTemplate template = MessageTemplate.MatchOntology("dictionary");
        final ACLMessage message = agent.receive(template);
        if (message == null) {
            block();
        } else {
            final String[] messageContent;
            String serviceName = null;
            final String content;

            if (message.getContent().contains(":")) {
                messageContent = message.getContent().split(":");
                serviceName = messageContent[0];
                content = messageContent[1];
            } else {
                content = message.getContent();
            }

            if( serviceName == null || !databases.contains(serviceName))
                serviceName = "wn";

            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            String response;
            try {
                response = agent.makeRequest(serviceName, content);
            } catch (NumberFormatException ex) {
                response = ex.getMessage();
            }
            reply.setContent(response);
            agent.send(reply);
        }
    }
}
