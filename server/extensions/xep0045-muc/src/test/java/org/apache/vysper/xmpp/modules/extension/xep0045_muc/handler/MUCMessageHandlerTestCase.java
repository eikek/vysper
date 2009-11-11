package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import static org.apache.vysper.xmpp.stanza.MessageStanzaType.GROUPCHAT;

import java.util.Arrays;

import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Role;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Decline;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Invite;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.Password;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.stanzas.X;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;
import org.apache.vysper.xmpp.xmlfragment.XMLFragment;
import org.apache.vysper.xmpp.xmlfragment.XMLText;

/**
 */
public class MUCMessageHandlerTestCase extends AbstractMUCMessageHandlerTestCase {

    public void testMessageWithNoVoice() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        Occupant occupant = room.addOccupant(OCCUPANT1_JID, "nick");
        // make sure the occupant has no voice
        occupant.setRole(Role.Visitor);

        testNotAllowedMessage(room, "forbidden");
    }

    public void testMessageUserNotOccupant() throws Exception {
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        // do not add user to room

        testNotAllowedMessage(room, "not-acceptable");
    }

    public void testMessageToRoomWithRelays() throws Exception {
        String body = "Message body";

        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to room
        sendMessage(OCCUPANT1_JID, ROOM1_JID, GROUPCHAT, body);

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT1_JID, "groupchat", body,
                occupant1Queue.getNext());
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "groupchat", body,
                occupant2Queue.getNext());
    }

    public void testMessageToOccupant() throws Exception {
        String body = "Message body";

        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to occupant 1
        sendMessage(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "Nick 2"), MessageStanzaType.CHAT, body);

        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID_WITH_NICK, OCCUPANT2_JID, "chat", body,
                occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }

    
    public void testGroupChatMessageToOccupant() throws Exception {
        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT1_JID, "nick");
        room.addOccupant(OCCUPANT2_JID, "Nick 2");

        // send message to occupant 1 with type groupchat
        Stanza errorStanza = sendMessage(OCCUPANT1_JID, new EntityImpl(ROOM1_JID, "Nick 2"), MessageStanzaType.GROUPCHAT, BODY);

        XMLText text = new XMLText(BODY);
        XMLElement expectedBody = new XMLElement("body", null,
                null, new XMLFragment[] { text });
        assertMessageErrorStanza(errorStanza, ROOM1_JID, OCCUPANT1_JID, "modify",
                "bad-request", expectedBody);

        // no message should be relayed
        assertNull(occupant1Queue.getNext());
        assertNull(occupant2Queue.getNext());

    }

    public void testInviteMessageWithPassword() throws Exception {
        String reason = "Join me!";

        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.setPassword("secret");
        room.addOccupant(OCCUPANT1_JID, "nick");

        Invite invite = new Invite(null, OCCUPANT2_JID, reason);
        // send message to occupant 1
        assertNull(sendMessage(OCCUPANT1_JID, ROOM1_JID, null, null, new X(invite), null));

        X expectedX = new X(new Invite(OCCUPANT1_JID, null, reason), new Password("secret"));
        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID, OCCUPANT2_JID, null, null, null, expectedX,
                occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }

    public void testDeclineMessage() throws Exception {
        String reason = "No way";

        // add occupants to the room
        Room room = conference.findOrCreateRoom(ROOM1_JID, "Room 1");
        room.addOccupant(OCCUPANT2_JID, "nick");

        Decline decline = new Decline(null, OCCUPANT2_JID, reason);
        // send message to occupant 1
        Stanza error = sendMessage(OCCUPANT1_JID, ROOM1_JID, null, null, new X(decline), null);
        assertNull(error);

        X expectedX = new X(new Decline(OCCUPANT1_JID, null, reason));
        // verify stanzas to existing occupants on the exiting user
        assertMessageStanza(ROOM1_JID, OCCUPANT2_JID, null, null, null, expectedX,
                occupant2Queue.getNext());
        assertNull(occupant1Queue.getNext());
    }
}
