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
package org.jboss.remoting.samples.transporter.complex.client;

import org.jboss.remoting.samples.transporter.complex.Doctor;
import org.jboss.remoting.samples.transporter.complex.NoDoctorAvailableException;
import org.jboss.remoting.samples.transporter.complex.Patient;
import org.jboss.remoting.samples.transporter.complex.ProviderInterface;
import org.jboss.remoting.transporter.TransporterClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client
{
   private String locatorURI = "socket://localhost:5401/?serializationtype=jboss";

   public void makeClientCall() throws Exception
   {
      // First create patient and populate with data
      Patient patient = new Patient("Bill", "Gates");
      patient.setAilmentType("financial");
      patient.setAilmentDescription("Money coming out the wazoo.");

      System.out.println("*** Have a new patient that needs a doctor.  The patient is:\n" + patient);

      // now create remote provide interface to call on
      ProviderInterface providerProcessor = (ProviderInterface) TransporterClient.createTransporterClient(locatorURI, ProviderInterface.class);


      try
      {
         // find a doctor that can help our patient.  Note, if none found, will throw an exception
         System.out.println("*** Looking for doctor that can help our patient...\n");
         Doctor doctor = providerProcessor.findDoctor(patient);

         // notice that list of patients now includes our patient, Bill Gates
         System.out.println("*** Found doctor for our patient.  Doctor found is:\n" + doctor);

         // assign doctor as patient's doctor
         patient.setDoctor(doctor);
         System.out.println("*** Set doctor as patient's doctor.  Patient info is now:\n" + patient);

         // let's say our doctor made enough money to retire after helping out our patient, Bill.
         providerProcessor.retireDoctor(doctor);

         // let's create a new patient and find a doctor for him
         Patient patient2 = new Patient("Larry", "Page");
         patient2.setAilmentType("financial");
         patient2.setAilmentDescription("Money coming out the wazoo.");

         System.out.println("*** Have a new patient that we need to find a doctor for (remember, the previous one retired and there are no others)");
         providerProcessor.findDoctor(patient2);
      }
      catch(NoDoctorAvailableException e)
      {
         System.out.println("*** Could not find doctor for patient.  This is an expected exception when there are not doctors available.");
         e.printStackTrace();
      }

      TransporterClient.destroyTransporterClient(providerProcessor);

   }


   static public void main(String[] args)
   {
      Client client = new Client();
      try
      {
         client.makeClientCall();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}

