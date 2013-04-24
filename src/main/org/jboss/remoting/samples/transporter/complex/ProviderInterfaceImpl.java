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
package org.jboss.remoting.samples.transporter.complex;

import org.jboss.remoting.samples.transporter.complex.Doctor;
import org.jboss.remoting.samples.transporter.complex.Information;
import org.jboss.remoting.samples.transporter.complex.NoDoctorAvailableException;
import org.jboss.remoting.samples.transporter.complex.Patient;
import org.jboss.remoting.samples.transporter.complex.ProviderInterface;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ProviderInterfaceImpl implements ProviderInterface
{
   public Doctor findDoctor(Patient patient) throws NoDoctorAvailableException
   {
      // get the patient's ailment type and look for doctor that specializes
      String ailmentType = patient.getAilmentType();

      Information info = Information.getInstance();
      Doctor doc = info.getProviderBySpecialty(ailmentType);
      if(doc != null)
      {
         doc.addPatient(patient);
         return doc;
      }
      else
      {
         throw new NoDoctorAvailableException("No doctor available for ailment '" + ailmentType + "'");
      }

   }

   public void retireDoctor(Doctor doctor)
   {
      Information info = Information.getInstance();
      info.retireDoctor(doctor);
   }

}
