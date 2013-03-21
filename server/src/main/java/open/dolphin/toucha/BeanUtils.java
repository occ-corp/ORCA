package open.dolphin.toucha;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;

/**
 *
 * @author Kazushi Minagawa.
 */
public class BeanUtils {

    private static final String UTF8 = "UTF-8";
    
    public static String beanToXml(Object bean)  {
        
        try {
            return new String(xmlEncode(bean), UTF8);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    public static Object xmlToBean(String beanXml) {

        try {
            byte[] bytes = beanXml.getBytes(UTF8);
            return xmlDecode(bytes);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    public static byte[] xmlEncode(Object bean)  {
        
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(bo);
        e.writeObject(bean);
        e.close();
        
        return bo.toByteArray();
    }
    
    public static Object xmlDecode(byte[] bytes) {
        
        XMLDecoder d = new XMLDecoder(new ByteArrayInputStream(bytes));
        return d.readObject();
    }
    
    public static Object deepCopy(Object src) {
  
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(src);
            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(byteIn);
            return in.readObject();
            
        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        }
        
        return null;
    }

/*
//masuda^   http://forums.sun.com/thread.jspa?threadID=427879

    public static byte[] xmlEncode(Object bean)  {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(new BufferedOutputStream(bo));
        
//masuda^   java.sql.Dateとjava.sql.TimestampがxmlEncodeで失敗する
        DatePersistenceDelegate dpd = new DatePersistenceDelegate();
        e.setPersistenceDelegate(java.sql.Date.class, dpd);
        TimestampPersistenceDelegate tpd = new TimestampPersistenceDelegate();
        e.setPersistenceDelegate(java.sql.Timestamp.class, tpd);
//masuda$

        e.writeObject(bean);
        e.close();
        return bo.toByteArray();
    }

   private static class DatePersistenceDelegate extends PersistenceDelegate {

       @Override
       protected Expression instantiate(Object oldInstance, Encoder out) {
           java.sql.Date date = (java.sql.Date) oldInstance;
           long time = Long.valueOf(date.getTime());
           return new Expression(date, date.getClass(), "new", new Object[]{time});
       }
   }

   private static class TimestampPersistenceDelegate extends PersistenceDelegate {

       @Override
       protected Expression instantiate(Object oldInstance, Encoder out) {
           java.sql.Timestamp date = (java.sql.Timestamp) oldInstance;
           long time = Long.valueOf(date.getTime());
           return new Expression(date, date.getClass(), "new", new Object[]{time});
       }
   }
//masuda$
*/
}
