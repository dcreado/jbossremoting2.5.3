package org.jboss.remoting.samples.transporter.complex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.remoting.samples.transporter.complex.Doctor;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class Information
{
   private static Information info = new Information();
   private static Map providers = new HashMap();
   private static List patients = new ArrayList();

   static
   {
      Doctor p1 = new Doctor("Andy Jones", "financial");
      providers.put("financial", p1);
      Doctor p2 = new Doctor("Joe Smith", "medical");
      providers.put("medical", p2);

      Patient pat1 = new Patient("Larry", "Ellison");
      pat1.setDoctor(p1);
      p1.addPatient(pat1);
      patients.add(pat1);
      Patient pat2 = new Patient("Steve", "Jobs");
      pat2.setDoctor(p1);
      p1.addPatient(pat2);
   }

   public static Information getInstance()
   {
      return info;
   }

   public Doctor getProviderBySpecialty(String ailmentType)
   {
      return (Doctor) providers.get(ailmentType);
   }

   public void retireDoctor(Doctor doctor)
   {
      String specialty = doctor.getSpecialty();
      providers.remove(specialty);
   }
}
