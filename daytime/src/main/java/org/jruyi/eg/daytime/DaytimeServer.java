/**
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jruyi.eg.daytime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

@Component(name = "jruyi.eg.daytime",
	immediate = true, // Immediate component
	createPid = false, // Don't create property service.pid
	metatype = true // Create metatype descriptor file
)
@Properties({
	// This IO service is identified with jruyi.eg.daytime.
	@Property(name = IoConstants.SERVICE_ID, value = "jruyi.eg.daytime",
		/* non-configurable property */ propertyPrivate = true),
	// Configurable property for the address to be bound to
	// Bound to any local address by default
	@Property(name = "bindAddr", value = "0.0.0.0"),
	// Configurable property for the listening port
	// Listen on port 9013 by default
	@Property(name = "port", intValue = 9013),
	// Configurable property for enabling/disabling Nagle's algorithm
	// Disable Nagle's algorithm by default
	@Property(name = "tcpNoDelay", boolValue = true) })
public class DaytimeServer extends SessionListener {

	// Specify the dependency service
	@Reference(name = "tcpServerFactory", target = "("
			+ ComponentConstants.COMPONENT_NAME + "="
			+ IoConstants.CN_TCPSERVER_FACTORY + ")")
	private ComponentFactory m_tcpServerFactory;

	private ComponentInstance m_tcpServer;
	private ISessionService m_ss;

	// This method is called when a connection is established
	// and ready to send/recv data.
	@Override
	public void onSessionOpened(ISession session) {
		// Current date and time
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEEE, MMMM d, yyyy HH:mm:ss-zzz", Locale.ENGLISH);
		String daytime = dateFormat.format(new Date());

		// Allocate the buffer and construct the message
		IBuffer out = session.createBuffer();
		out.write(daytime, Codec.us_ascii());
		out.write("\r\n", Codec.us_ascii());

		// Send out the message
		m_ss.write(session, out);
	}

	// This method is called after the given msg is sent.
	@Override
	public void onMessageSent(ISession session, Object msg) {
		// Close the connection
		m_ss.closeSession(session);
	}

	protected void bindTcpServerFactory(ComponentFactory factory) {
		m_tcpServerFactory = factory;
	}

	protected void unbindTcpServerFactory(ComponentFactory factory) {
		m_tcpServerFactory = null;
	}

	// This method is called when this component is being activated.
	protected void activate(Map<String, ?> properties) throws Exception {
		// Create the Session Service of tcpserver
		ComponentInstance tcpServer = m_tcpServerFactory
				.newInstance(new Hashtable<String, Object>(properties));
		ISessionService ss = (ISessionService) tcpServer.getInstance();

		// Set SessionListener
		ss.setSessionListener(this);

		// Start the Session Service
		ss.start();
		m_tcpServer = tcpServer;
		m_ss = ss;
	}

	// This method is called when this component is being deactivated.
	protected void deactivate() {
		// Stop the Session Service
		m_tcpServer.dispose();
		m_tcpServer = null;
		m_ss = null;
	}
}
