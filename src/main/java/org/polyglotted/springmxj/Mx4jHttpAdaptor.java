package org.polyglotted.springmxj;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import mx4j.tools.adaptor.AdaptorServerSocketFactory;
import mx4j.tools.adaptor.http.HttpAdaptor;
import mx4j.tools.adaptor.http.ProcessorMBean;
import mx4j.tools.adaptor.http.XSLTProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource("org.polyglotted.springjmx:name=SpringMx4jHttpClient")
public class Mx4jHttpAdaptor extends HttpAdaptor {

    private static final Logger Logger = LoggerFactory.getLogger(Mx4jHttpAdaptor.class);
    private static final String BasicAuth = "basic";
    private static final String XsltProcessorName = "Server:name=Mx4jXSLTProcessor";
    private static final XSLTProcessor DefaultXsltProcessor = new XSLTProcessor();

    private String jmxNicInterface;
    private InetAddress jmxHostAddress;
    private String userName;
    private String password;
    private String passPhrase;

    public Mx4jHttpAdaptor() {
        setPort(-1);
    }

    @PostConstruct
    public void init() {
        checkArgument(getPort() != -1, "must specify jmx Http Port");
        addAuthorize();
        setSocketFactory();
        addShutdownHook();
    }

    private void addAuthorize() {
        checkNotNull(userName, "userName cannot be null");
        checkNotNull(password, "password cannot be null and should be encrypted");
        checkNotNull(passPhrase, "passPhrase cannot be null");
        super.setAuthenticationMethod(BasicAuth);
        super.addAuthorization(userName, password); // should be unencrypted here
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Mx4jHttpAdaptor.this.stop();
            }
        });
    }

    private void setSocketFactory() {
        jmxHostAddress = nonLoopbackAddress();
        super.setSocketFactory(new AdaptorServerSocketFactory() {
            @Override
            public ServerSocket createServerSocket(int port, int backlog, String host) throws IOException {
                Logger.info("Created mx4j ServerSocket on: " + jmxHostAddress + ":" + port);
                return new ServerSocket(port, backlog, jmxHostAddress);
            }
        });
    }

    private InetAddress nonLoopbackAddress() {
        InetAddress address = getHostBindToAddress();
        if (address != null) {
            if (!address.isLoopbackAddress()) {
                return address;
            }
            else
                Logger.warn("Jmx will not bind to loopback address: " + jmxNicInterface);
        }
        else
            Logger.info("Didn't find address for nic: " + jmxNicInterface);

        try {
            return InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("unable to load mx4j http", e);
        }
    }

    private InetAddress getHostBindToAddress() {
        InetAddress resultAddress = null;
        if (jmxNicInterface != null) {
            try {
                return InetAddress.getByName(jmxNicInterface);
            }
            catch (UnknownHostException ex) {
                Logger.info(jmxNicInterface + " does not map to a host");
            }

            try {
                NetworkInterface nic = NetworkInterface.getByName(jmxNicInterface);
                if (nic != null) {
                    Enumeration<InetAddress> nicInetAddresses = nic.getInetAddresses();
                    if (nicInetAddresses.hasMoreElements()) {
                        resultAddress = nicInetAddresses.nextElement();
                        Logger.info("Using ip address " + resultAddress + " for nic " + jmxNicInterface);

                        while (nicInetAddresses.hasMoreElements()) {
                            Logger.info("Other potential ip address for " + jmxNicInterface + ": "
                                    + nicInetAddresses.nextElement());
                        }
                    }
                }
            }
            catch (SocketException ex) {
                Logger.warn("Exception while looking up nic Interface: " + jmxNicInterface, ex);
            }
        }
        return resultAddress;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws java.lang.Exception {
        ObjectName objectName = super.preRegister(server, name);
        super.setProcessorNameString(XsltProcessorName);

        ProcessorMBean xsltProcessor = getProcessor() == null ? DefaultXsltProcessor : getProcessor();
        super.setProcessor(xsltProcessor);
        server.registerMBean(xsltProcessor, new ObjectName(XsltProcessorName));

        super.start();
        Logger.info("Started Mx4j http client");
        return objectName;
    }

    @Override
    public void stop() {
        synchronized (this) {
            super.stop();
        }
    }

    public String getJmxNicInterface() {
        return jmxNicInterface;
    }

    public void setJmxNicInterface(String jmxNicInterface) {
        this.jmxNicInterface = jmxNicInterface;
    }

    public InetAddress getJmxHostAddress() {
        return jmxHostAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassPhrase() {
        return passPhrase;
    }

    public void setPassPhrase(String passPhrase) {
        this.passPhrase = passPhrase;
    }

    @Override
    public void setHost(String host) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuthenticationMethod(String method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessor(ProcessorMBean processor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessorClass(String processorClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessorNameString(String processorName) throws MalformedObjectNameException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessorName(ObjectName processorName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSocketFactory(AdaptorServerSocketFactory factory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSocketFactoryName(ObjectName factoryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSocketFactoryNameString(String factoryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAuthorization(String username, String password) {
        throw new UnsupportedOperationException();
    }
}