package com.wso2telco.dep.mediator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlAlterMediator extends AbstractMediator {
    private Log log = LogFactory.getLog(this.getClass());

	public boolean mediate(MessageContext context) {
		Document doc;
		try {
			doc = getDocumentByXml(context.getMessageID().toString());//TODO
			//StringBuilder builder=new StringBuilder();
			String []removeElements={"toAddress","authorizationHeader","operatorId"};//TODO
			for (String element : removeElements) {
				deleteElements(doc,element);
				log.info(element);
			}
			xmlDocPrint(doc);
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
		} catch (SAXException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return false;
	}


	private Document getDocumentByXml(String xml) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xml));
		Document doc = db.parse(is);
		doc.getDocumentElement().normalize();
		return doc;
	}

	private final void xmlDocPrint(Document xml) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		log.info(out.toString());
	}

	private Document deleteElements(Document doc, String element) {
		NodeList nodeList = doc.getElementsByTagName(element);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				node.getParentNode().removeChild(node);
			}
		}
		return doc;
	}
}
