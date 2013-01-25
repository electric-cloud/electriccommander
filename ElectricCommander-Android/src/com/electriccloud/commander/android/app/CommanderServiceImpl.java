/*
 * Copyright (C) 2012 Electric Cloud, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.electriccloud.commander.android.app;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.transport.XmlSerializer;

public class CommanderServiceImpl
{

    //~ Instance fields --------------------------------------------------------

    private XmlSerializer m_xmlSerializer = new XmlSerializer();
    private String        m_sessionId     = "";

    //~ Methods ----------------------------------------------------------------

    public byte[] createRequest(CommanderRequest<?> request)
        throws UnsupportedEncodingException
    {
        String actualRequest = createEnvelope(m_xmlSerializer.serialize(
                    request));

        return actualRequest.getBytes("UTF-8");
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    public InputStream createStream(
            String urlString,
            byte[] postData)
    {
        URL url = null;

        try {
            url = new URL(urlString);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection conn;
        InputStream       stream = null;

        try {
            conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

            OutputStream out = new BufferedOutputStream(conn.getOutputStream());

            out.write(postData);
            out.close();

            // Starts the query
            conn.connect();
            stream = conn.getInputStream();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return stream;
    }

    private String createEnvelope(String xmlFragment)
    {
        return
            "<requests xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"commander.xsd\" version=\"2.2\" timeout=\"180\" sessionId=\""
                + m_sessionId + "\">" + xmlFragment.toString() + "</requests>";
    }

    public String getSessionId()
    {
        return m_sessionId;
    }

    public void setSessionId(String m_sessionId)
    {
        this.m_sessionId = m_sessionId;
    }
}
