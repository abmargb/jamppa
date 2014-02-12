/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smack;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.util.Base32Encoder;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Roster;
import org.xmpp.packet.Roster.Ask;
import org.xmpp.packet.Roster.Item;
import org.xmpp.packet.Roster.Subscription;

/**
 * Stores roster entries as specified by RFC 6121 for roster versioning
 * in a set of files.
 *
 * @author Lars Noschinski
 * @author Fabian Schuetz
 */
public class DefaultRosterStore implements RosterStore {

    private final File fileDir;

    private static final String ENTRY_PREFIX = "entry-";
    private static final String VERSION_FILE_NAME = "__version__";
    private static final String STORE_ID = "DEFAULT_ROSTER_STORE";

    private static final FileFilter rosterDirFilter = new FileFilter() {

        @Override
        public boolean accept(File file) {
            String name = file.getName();
            return name.startsWith(ENTRY_PREFIX);
        }

    };

    /**
     * @param baseDir
     *            will be the directory where all roster entries are stored. One
     *            file for each entry, such that file.name = entry.username.
     *            There is also one special file '__version__' that contains the
     *            current version string.
     */
    private DefaultRosterStore(final File baseDir) {
        this.fileDir = baseDir;
    }

    /**
     * Creates a new roster store on disk
     *
     * @param baseDir
     *            The directory to create the store in. The directory should
     *            be empty
     * @return A {@link DefaultRosterStore} instance if successful,
     *         <code>null</code> else.
     */
    public static DefaultRosterStore init(final File baseDir) {
        DefaultRosterStore store = new DefaultRosterStore(baseDir);
        if (store.setRosterVersion("")) {
            return store;
        }
        else {
            return null;
        }
    }

    /**
     * Opens a roster store
     * @param baseDir
     *            The directory containing the roster store.
     * @return A {@link DefaultRosterStore} instance if successful,
     *         <code>null</code> else.
     */
    public static DefaultRosterStore open(final File baseDir) {
        DefaultRosterStore store = new DefaultRosterStore(baseDir);
        String s = store.readFile(store.getVersionFile());
        if (s != null && s.startsWith(STORE_ID + "\n")) {
            return store;
        }
        else {
            return null;
        }
    }

    private File getVersionFile() {
        return new File(fileDir, VERSION_FILE_NAME);
    }

    @Override
    public List<Item> getEntries() {
        List<Item> entries = new ArrayList<Roster.Item>();

        for (File file : fileDir.listFiles(rosterDirFilter)) {
            Item entry = readEntry(file);
            if (entry == null) {
                log("Roster store file '" + file + "' is invalid.");
            }
            else {
                entries.add(entry);
            }
        }

        return entries;
    }

    @Override
    public Item getEntry(String bareJid) {
        return readEntry(getBareJidFile(bareJid));
    }

    @Override
    public String getRosterVersion() {
        String s = readFile(getVersionFile());
        if (s == null) {
            return null;
        }
        String[] lines = s.split("\n", 2);
        if (lines.length < 2) {
            return null;
        }
        return lines[1];
    }

    private boolean setRosterVersion(String version) {
        return writeFile(getVersionFile(), STORE_ID + "\n" + version);
    }

    @Override
    public boolean addEntry(Item item, String version) {
        return addEntryRaw(item) && setRosterVersion(version);
    }

    @Override
    public boolean removeEntry(String bareJid, String version) {
        try {
            return getBareJidFile(bareJid).delete() && setRosterVersion(version);
        }
        catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean resetEntries(Collection<Item> items, String version) {
        try {
            for (File file : fileDir.listFiles(rosterDirFilter)) {
                file.delete();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
        for (Item item : items) {
            if (!addEntryRaw(item)) {
                return false;
            }
        }
        return setRosterVersion(version);
    }

    private Item readEntry(File file) {
        String s = readFile(file);
        if (s == null) {
            return null;
        }

        String user = null;
        String name = null;
        String type = null;
        String status = null;

        List<String> groupNames = new ArrayList<String>();

        try {
            XmlPullParser parser = new MXParser();
            parser.setInput(new StringReader(s));

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("item")) {
                        user = parser.getAttributeValue(null, "user");
                        name = parser.getAttributeValue(null, "name");
                        type = parser.getAttributeValue(null, "type");
                        status = parser.getAttributeValue(null, "status");
                    }
                    if (parser.getName().equals("group")) {
                        String group = parser.getAttributeValue(null, "name");
                        if (group != null) {
                            groupNames.add(group);
                        }
                        else {
                            log("Invalid group entry in store entry file "
                                    + file);
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("item")) {
                        done = true;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();;
            return null;
        }
        catch (XmlPullParserException e) {
            log("Invalid group entry in store entry file "
                    + file);
            e.printStackTrace();
            return null;
        }

        if (user == null) {
            return null;
        }
        
        Subscription subscription = null;
        Ask ask = null;

        if (type != null) {
            try {
                subscription = Subscription.valueOf(type);
            }
            catch (IllegalArgumentException e) {
                log("Invalid type in store entry file " + file);
                return null;
            }
            if (status != null) {
                ask = Ask.valueOf(status);
                if (ask == null) {
                    log("Invalid status in store entry file " + file);
                    return null;
                }
            }
        }

        Roster roster = new Roster();
        roster.addItem(new JID(user), name, ask, subscription, groupNames);
        return roster.getItems().iterator().next();
    }


    private boolean addEntryRaw (Item item) {
        StringBuilder s = new StringBuilder();
        s.append("<item ");
        s.append(StringUtils.xmlAttrib("user", item.getJID().toBareJID()));
        s.append(" ");
        if (item.getName() != null) {
            s.append(StringUtils.xmlAttrib("name", item.getName()));
            s.append(" ");
        }
        if (item.getSubscription() != null) {
            s.append(StringUtils.xmlAttrib("type", item.getSubscription().name()));
            s.append(" ");
        }
        if (item.getAsk() != null) {
            s.append(StringUtils.xmlAttrib("status", item.getAsk().toString()));
            s.append(" ");
        }
        s.append(">");
        for (String group : item.getGroups()) {
            s.append("<group ");
            s.append(StringUtils.xmlAttrib("name", group));
            s.append(" />");
        }
        s.append("</item>");
        return writeFile(getBareJidFile(item.getJID().toBareJID()), s.toString());
    }


    private File getBareJidFile(String bareJid) {
        String encodedJid = Base32Encoder.getInstance().encode(bareJid);
        return new File(fileDir, ENTRY_PREFIX + encodedJid);
    }

    private String readFile(File file) {
        try {
            Reader reader = null;
            try {
                char buf[] = new char[8192];
                int len;
                StringBuilder s = new StringBuilder();
                reader = new FileReader(file);
                while ((len = reader.read(buf)) >= 0) {
                    s.append(buf, 0, len);
                }
                return s.toString();
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        catch (FileNotFoundException e) {
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean writeFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file, false);
            writer.write(content);
            writer.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void log(String error) {
        System.err.println(error);
    }
}
