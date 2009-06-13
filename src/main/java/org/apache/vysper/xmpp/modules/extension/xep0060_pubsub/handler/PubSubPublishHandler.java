/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.CollectionNode;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.model.LeafNode;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;


/**
 * @author The Apache MINA Project (http://mina.apache.org)
 *
 */
public class PubSubPublishHandler extends AbstractPubSubGeneralHandler {

	/**
	 * @param root
	 */
	public PubSubPublishHandler(CollectionNode root) {
		super(root);
	}

	@Override
	protected String getWorkerElement() {
		return "publish";
	}

	@Override
	protected Stanza handleSet(IQStanza stanza,
			ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		Entity sender = stanza.getFrom();
		Entity receiver = stanza.getTo();

		String iqStanzaID = stanza.getAttributeValue("id");
		
		StanzaBuilder sb = StanzaBuilder.createIQStanza(receiver, sender, IQStanzaType.RESULT, iqStanzaID);
		sb.startInnerElement("pubsub", NamespaceURIs.XEP0060_PUBSUB);
		
		XMLElement publish = stanza.getFirstInnerElement().getFirstInnerElement(); // pubsub/publish
		String strNode = publish.getAttributeValue("node"); // MUST

		XMLElement item = publish.getFirstInnerElement();
		String strID = item.getAttributeValue("id"); // MAY
		
		Entity jid = new EntityImpl(receiver.getNode(), receiver.getDomain(), strNode);
		LeafNode node = root.find(jid);
		
		if(node == null) {
			//TODO node does not exist - error condition 3 (7.1.3)
			return null;
		}
		
		if(!node.isSubscribed(sender)) {
			// TODO not enough privileges to publish - error condition 1 (7.1.3)
			return null;
		}
		
		if(strID == null) {
			strID = idGenerator.create();
			// wrap a new item element with the id attribute
			StanzaBuilder itemBuilder = new StanzaBuilder("item");
			itemBuilder.addAttribute("id", strID);
			itemBuilder.addPreparedElement(item.getFirstInnerElement());
			item = itemBuilder.getFinalStanza();
		}
		
		StanzaRelay relay = serverRuntimeContext.getStanzaRelay();
		node.publish(sender, relay, strID, item);
		
		buildSuccessStanza(sb, strNode, strID);
		
		sb.endInnerElement(); // pubsub
		return new IQStanza(sb.getFinalStanza());
	}
	
	private void buildSuccessStanza(StanzaBuilder sb, String node, String id) {
		sb.startInnerElement("publish");
		sb.addAttribute("node", node);
		
		sb.startInnerElement("item");
		sb.addAttribute("id", id);
		sb.endInnerElement();
		
		sb.endInnerElement();
	}
}