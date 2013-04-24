package org.jboss.remoting.samples.transporter.complex;

import org.jboss.remoting.samples.transporter.complex.Doctor;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class Patient
{
   private String firstName = null;
   private String lastName = null;
   private Doctor doctor = null;
   private String ailmentType = null;
   private String ailmentDescription = null;

   private Patient()
   {

   }

   public Patient(String firstName, String lastName)
   {
      this.firstName = firstName;
      this.lastName = lastName;
   }

   public void setDoctor(Doctor doctor)
   {
      this.doctor = doctor;
   }

   public String toString()
   {
      return "\nPatient:\n\tName: " + firstName + " " + lastName + "\n\tAilment - Type: " + ailmentType +
             ", Description: " + ailmentDescription + (doctor != null ? ("\n\tDoctor - Name: " + doctor.getFullName()) : "") + "\n";
   }

   public void setAilmentType(String ailmentType)
   {
      this.ailmentType = ailmentType;
   }

   public void setAilmentDescription(String ailmentDesc)
   {
      this.ailmentDescription = ailmentDesc;
   }

   public String getAilmentType()
   {
      return ailmentType;
   }
}
