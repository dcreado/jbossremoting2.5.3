package org.jboss.remoting.samples.chat.client;

import org.jboss.remoting.samples.chat.exceptions.ConnectionException;

public interface ConnectionStrategy
{
    void list() throws ConnectionException;
    void create() throws ConnectionException;
}
