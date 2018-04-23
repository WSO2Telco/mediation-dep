package com.wso2telco.dep.mediator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlAlterMediator extends AbstractMediator {
  private String removeEl;

	public String getRemoveEl() {
		return removeEl;
	}

	public void setRemoveEl(String removeEl) {
		this.removeEl = removeEl;
	}

	Document document;
	public boolean mediate(MessageContext context) {
		String responsePayload=null;
		try {
			document = getDocumentByXml(context.getEnvelope().toString());
			if(removeEl.contains("_")) {
				String []elements = removeEl.split("_");
				for (String element : elements) {
					removeParentChild(element);
				}
			}else {
				removeParentChild(removeEl);
			}

			responsePayload=xmlDocPrint(document);

	        SOAPBody body = context.getEnvelope().getBody();
	        OMElement firstChild = body.getFirstElement();
	        OMElement omXML = AXIOMUtil.stringToOM(responsePayload);
	        if (firstChild == null) {
	            body.addChild(omXML);
	        } else {
	            firstChild.insertSiblingAfter(omXML);
	            firstChild.detach();
	        }
		} catch (ParserConfigurationException|SAXException|IOException|XMLStreamException|TransformerException e) {
			log.error(e.getMessage());
		}
		return true;
	}

	private void removeParentChild(String removeElement) {
		String[] elementTmp = removeElement.split("\\.");
		String parentElement = elementTmp[0];
		String childElement = elementTmp[1];
		deleteElements(parentElement, childElement);
		log.info(removeElement);
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

	private final String xmlDocPrint(Document xml) throws TransformerException {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		log.info(out.toString());
		return out.toString();
	}

	private Document deleteElements(String parentElement,String childElement) {
		NodeList nodeList = document.getElementsByTagName(childElement);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if ((node.getNodeType() == Node.ELEMENT_NODE)&&(node.getParentNode().getNodeName().equals(parentElement))) {
				node.getParentNode().removeChild(node);
			}
		}
		return document;
	}
}
