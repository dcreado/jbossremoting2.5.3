/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.remoting.serialization.impl.jboss;

import java.io.IOException;

import org.jboss.remoting.serialization.IMarshalledValue;
import org.jboss.serial.io.MarshalledObjectForLocalCalls;
import org.jboss.serial.objectmetamodel.safecloning.SafeCloningRepository;

/**
 * 
 * IMarshalledValue used on smart cloning/call by value operations for JBoss Serialization
 * @author Clebert Suconic
 *
 */
public class SmartCloningMarshalledValue extends MarshalledObjectForLocalCalls implements IMarshalledValue 
{

	public SmartCloningMarshalledValue() {
		super();
	}

	public SmartCloningMarshalledValue(Object arg0, SafeCloningRepository arg1)
			throws IOException {
		super(arg0, arg1);
	}

	public SmartCloningMarshalledValue(Object arg0) throws IOException {
		super(arg0);
	}

}