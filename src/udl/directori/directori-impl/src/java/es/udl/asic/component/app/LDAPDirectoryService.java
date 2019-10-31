package es.udl.asic.component.app.directori;

import java.util.Properties;
import es.udl.asic.api.app.directori.*;
import es.udl.utilities.ldap.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
//Codi per les connexions al servei de missatgeria
import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope; 
import org.apache.commons.httpclient.NameValuePair;



public class LDAPDirectoryService implements DirectoryService {

	LDAPUdLUtils ldapUdlUtils = null;

	String base = "",baseRoot="",baseAlu="";
	String aluDiscUrl = "";
	String aluDiscUsername = "";
	String aluDiscPassword = "";

	public void configParameters(Properties properties)
			throws ParameterConfigException {

		/* Get the init parameters */

		String ldapserverrwstr = properties.getProperty("ldapserver_rw");
		String ldapserverrostr = properties.getProperty("ldapserver_ro");
		String securityprincipalstr = properties.getProperty("securityPrincipal");
		String securitycredentialsstr = properties.getProperty("securityCredentials");
		base = properties.getProperty("base");
		baseRoot = properties.getProperty("baseRoot");
		baseAlu = properties.getProperty ("baseAlu");
		aluDiscUrl = properties.getProperty ("aluDiscUrl");
		aluDiscUsername = properties.getProperty ("aluDiscUsername");
		aluDiscPassword = properties.getProperty ("aluDiscPassword");
		

		if (ldapserverrwstr != null && ldapserverrostr != null
				&& securityprincipalstr != null
				&& securitycredentialsstr != null) {

			/* Create a new instance of ldap utilities */
			ldapUdlUtils = new LDAPUdLUtils();

			/* Initalize the config parameters */
			ldapUdlUtils.setLdapserver_rw(ldapserverrwstr);
			ldapUdlUtils.setLdapserver_ro(ldapserverrostr);
			ldapUdlUtils.setSecurityPrincipal(securityprincipalstr);
			ldapUdlUtils.setSecurityCredentials(securitycredentialsstr);
			// System.out.println("Dades configurades"+ ldapserverrwstr + " " +
			// ldapserverrostr + " " + securityprincipalstr + " " +
			// securitycredentialsstr );
		} else {
			throw new ParameterConfigException();
		}
	}
	
	public boolean canviPasswd(String userid, String newpasswd){
		return canviPasswd (userid,newpasswd,base);
	}

	public boolean canviPasswdAlu(String userid, String newpasswd){
		return canviPasswd (userid,newpasswd,baseAlu);
	}

	public boolean canviPasswd(String userid, String newpasswd,String basetype) {

		// The userid is null
		if (userid == null)
			return false;

		// Password is null
		if (newpasswd == null)
			return false;

		// Not valid password
		if (!validatePasswd(newpasswd))
			return false;
		try {
			if (ldapUdlUtils.canviar_passwd(userid, newpasswd, basetype) != 0) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}

		return true;
	}

	
	public boolean mailExisteix (String email,String userid){
		//Cerquem el login per obtenir el subdomini
		if (userid == null ||  email==null)
			return false;
		
		Attributes attrsread = null;
		SearchResult result = null;
		NamingEnumeration enumer = ldapUdlUtils.searchLDAP("(uid=" + userid
				+ ")", base);

		if (email.equals(""))
			return true;
		
		try{
			if (enumer.hasMore()){
				result = (SearchResult)  enumer.next();
				attrsread = result.getAttributes();
			}
		}catch (Exception ex){
			System.out.println("No te alternateAdress");
		}
		
		try{
			String correuprov = attrsread.get("mail").get().toString();
			
			int index = correuprov.indexOf("@");
			correuprov = correuprov.substring(index);
			String emailtotal = email + correuprov;
			return mailExisteix(emailtotal);
		}catch (Exception ex){
			System.out.println("No s'ha recuperat el camp de correu");
		}
		return false;
	}
	
	
	public boolean mailExisteix (String email){
		String filtre = "(&(objectclass=posixAccount)(mailAlternateAddress="+email+"))";

		NamingEnumeration enumer = ldapUdlUtils.searchLDAP(filtre,baseRoot);
		
		if (email.equals(""))
			return true;
		
		try{
			if(enumer.hasMore()){
				return true;
			}
		}catch(Exception ex){
			ex.printStackTrace ();
			System.out.println("Error al provar si existeix el correu");
		}
		
		return false;
	}
	
	public boolean activaCompte(String userid,String password,String email){

		if (userid == null || password==null || email==null)
			return false;
	
		
		//Canviem el password
		try {
			if (ldapUdlUtils.canviar_passwd(userid, password, base) != 0) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}

		//Fem la cerca
		

		//Llegim els camps del MailAlternateAddress		
		Attributes attrs = new BasicAttributes(false);
		Attribute mailAlternateAttribute = new BasicAttribute("mailAlternateAddress");
		Attributes attrsread = null;
		SearchResult result = null;
		NamingEnumeration enumer = ldapUdlUtils.searchLDAP("(uid=" + userid
				+ ")", base);

		try{
			if (enumer.hasMore()){
				result = (SearchResult)  enumer.next();
				attrsread = result.getAttributes();
			}
		}catch (Exception ex){
			System.out.println("No te alternateAdress");
		}
		
		email = email.toLowerCase();
		
		
		int inicies=-1,inicicat=-1;
		
		inicies = email.indexOf("udl.es");
		
		if (inicies==-1)
			inicicat = email.indexOf("udl.cat");
		
		String capalera = email.substring(0,inicies + inicicat +1); //+1 pq un dels 2 serà -1
		
		
		if (result!=null){
		
			try{
				Attribute mailAlternate = attrsread.get("mailAlternateAddress");
				
				if (mailAlternate != null){
					
				int size = mailAlternate.size();
				
					//Mirem si l'entrada ja està escrita. En el procés d'activació segur que no
					for (int i=0;i<size;i++){
						String alternate =  mailAlternate.get(i).toString().toLowerCase();
						if (!alternate.startsWith(capalera)){//Si es la que li passem no la gravem ja ho farem
							mailAlternateAttribute.add(alternate);
						}
					}
				}
			}catch (Exception ex){
					System.out.println("Error recuperant les dades");
					ex.printStackTrace();
					return false;
			}
		}
		
		
		mailAlternateAttribute.add(capalera+"udl.cat");

		//Ja no cal fincar la udl.es
		//mailAlternateAttribute.add(capalera+"udl.es");

		//Afegim el correu 
		attrs.put("mail", email);
		attrs.put(mailAlternateAttribute);

		
		if (!ldapUdlUtils.modifyLDAP("uid=" + userid + "," + base, attrs,
				DirContext.REPLACE_ATTRIBUTE))
			return false;
		
		
		//Esborrem l'entrada del camp de codi
		Attribute att   = new BasicAttribute("telexNumber");
		Attributes atts = new BasicAttributes(false);
		atts.put(att);
				
		ldapUdlUtils.modifyLDAP("uid=" + userid + "," + base, atts, DirContext.REMOVE_ATTRIBUTE);
		
		
		return true;
	}
	
	
	public boolean activaCompteAlu(String userid,String password){

		if (userid == null || password==null )
		{
			if (userid == null) 
				System.out.println ("ActivaAlu: userid null");
			if (password==null)
				System.out.println ("ActivaAlu: password null");
				
			return false;
		}
		
		//Canviem el password
		try {
			if (ldapUdlUtils.canviar_passwd(userid, password, baseAlu) != 0) {
				System.out.println ("ldapUtils error");
				return false;
			}
		} catch (Exception ex) {
			return false;
		}

		
		//Esborrem l'entrada del camp de codi
		
		Attributes atts = new BasicAttributes(false);
		atts.put("accountStatus","ACTIU");
				
		ldapUdlUtils.modifyLDAP("uid=" + userid + "," + baseAlu, atts, DirContext.REPLACE_ATTRIBUTE);
		
		
		//Intentem activar mitjançant una crida REST el espai de disc de l'alumne
		
		//activem l'espai de disc
		
		if (aluDiscUrl != null && !"".equals(aluDiscUrl)){
			try {
				HttpClient httpclient = authenticate();
				
				if (httpclient == null) {
					System.out.println ("ActivaAlu: Authentication to server failed");
					//return;
				}
				if (send(httpclient,userid)){
					System.out.println ("Activaalu Disc d'usuari activat: " + userid);
				} else{
					System.out.println ("Activaalu: Error de servidor al activar el disc de " + userid);
				}
				
			}catch (Exception ex){
				System.out.println ("Activaalu: hi ha hagut algun error al activar el disc de " + userid);
			}
		}
		
		
		return true;
	}
	
	
	public boolean canviDades(String userid, DadesPersonals dadesPersonals){
		return canviDades(userid,dadesPersonals,base);
	}
	
	public boolean canviDadesAlumne(String userid,DadesPersonals dadesPersonals){
		return canviDadesEstudiants(userid,dadesPersonals,baseAlu);
	}
	
	public boolean canviDadesArrel (String userid,DadesPersonals dadesPersonals){
		return canviDades (userid,dadesPersonals,baseRoot);
	}
	
	
	public boolean canviDadesEstudiants (String userid,DadesPersonals dadesPersonals,String basep){
		if (userid == null){
			return false;
		}	
		// DadesPersonals null
		if (dadesPersonals == null) {
			return false;
		}

		String mobile = dadesPersonals.getMobile();
		Attributes attrs = new BasicAttributes(false);
		
		if (mobile.trim().equals("")){
			mobile=null;
		}
		attrs.put("mobile", mobile);
		
		String reenviament,correuprincipal,correualternatiu;
		boolean guardacorreu;
		
		reenviament = dadesPersonals.getReenviament();
		correualternatiu = dadesPersonals.getCorreuAlternatiu();
		correuprincipal = dadesPersonals.getCorreuprincipal().toLowerCase();
		
		if (correualternatiu == null || "".equals(correualternatiu)){
			attrs.put("mailMessageStore", null);
		} else{
			attrs.put("mailMessageStore", correualternatiu);
		}
		
		//De moment desactivat
		Attribute mailForwardAttribute = new BasicAttribute("mailForwardingAddress");
		
		int size = mailForwardAttribute.size();
		
		if (reenviament.length()==0){
			guardacorreu=false;
			reenviament=null;
		}else{
			guardacorreu=true;
		}
		
		mailForwardAttribute.add(reenviament);
		// a part del correu extern es desa sempre l'adreça UdL també per conservar còpia dels correus als servidors UdL
		if (guardacorreu && reenviament!=null) {
			mailForwardAttribute.add(correuprincipal);
		}
		
		attrs.put(mailForwardAttribute);
	
		if (!ldapUdlUtils.modifyLDAP("uid=" + userid + "," + basep, attrs, DirContext.REPLACE_ATTRIBUTE)){
			System.out.println("No puc fer el replace");
			return false;
		}
		else
			return true;
		
	}
	
	public boolean canviDades(String userid, DadesPersonals dadesPersonals,String basep){
		// The userid is null
		

		if (userid == null){
			return false;
		}	
		// DadesPersonals null
		if (dadesPersonals == null) {
			System.out.println ("Les dades personals son null");
			return false;
		}

		if (!validateDades(dadesPersonals)) {
			return false;
		}

		String givenName, displayname, sn, gecos, cn, tlf, mobile, fax, webpersonal, ubicacio, correuprincipal, reenviament, missatgeria, correualternatiu;
		boolean guardacorreu;

		sn = dadesPersonals.getCognoms();
		givenName = dadesPersonals.getNom();
		cn = dadesPersonals.getNom() + " " + dadesPersonals.getCognoms();
		gecos = ldapUdlUtils.netejaString(cn);
		tlf = dadesPersonals.getTlf();
		mobile = dadesPersonals.getMobile();
		fax = dadesPersonals.getFax();
		webpersonal = dadesPersonals.getWebpersonal();
		ubicacio = dadesPersonals.getUbicacio();
		correuprincipal = dadesPersonals.getCorreuprincipal().toLowerCase();
		reenviament = dadesPersonals.getReenviament();
		guardacorreu = dadesPersonals.getGuardacorreu();
		missatgeria = dadesPersonals.getMissatgeria();
		correualternatiu = dadesPersonals.getCorreuAlternatiu();
		
		Attributes attrs = new BasicAttributes(false);
		// attrs.put("uid", Username);
		attrs.put("cn", cn);
		attrs.put("givenname", givenName);
		attrs.put("sn", sn);
		attrs.put("gecos", gecos);
		attrs.put("telephoneNumber", tlf);
		attrs.put("facsimileTelephoneNumber", fax);
		if (mobile.trim().equals("")){
			mobile=null;
		}
		attrs.put("mobile", mobile);
		
		attrs.put("registeredAddress", webpersonal);
		// Aquesta si que permetem que sigui buida (Hi ha gent per tot)
		if (ubicacio.equals(""))
			ubicacio = " ";
		attrs.put("roomNumber", ubicacio);

		// Guardem el correu
		attrs.put("mail", correuprincipal);
		
		if (correualternatiu == null || "".equals(correualternatiu)){
			attrs.put("mailMessageStore", null);
		} else{
			attrs.put("mailMessageStore", correualternatiu);
		}
		
		//Deso l'àlies de la missatgeria
		if (missatgeria.equals(""))	//pot estar buit
			missatgeria = null;
		attrs.put("pager", missatgeria);
		
		//De moment desactivat
		Attribute mailForwardAttribute = new BasicAttribute("mailForwardingAddress");
		
		int size = mailForwardAttribute.size();
		if (reenviament.length()==0){
			dadesPersonals.setGuardacorreu(false);
			reenviament=null;	
		}
		
		mailForwardAttribute.add(reenviament);
		// a part del correu extern es desa sempre l'adreça UdL també per conservar còpia dels correus als servidors UdL
		if (guardacorreu && reenviament!=null) {
			mailForwardAttribute.add(correuprincipal);
		}
		
		attrs.put(mailForwardAttribute);

		if (!ldapUdlUtils.modifyLDAP("uid=" + userid + "," + basep, attrs, DirContext.REPLACE_ATTRIBUTE)){
			return false;
		}
		else
			return true;
	}

	
	public  DadesPersonals obtenirDades(String userid,boolean isManager){
				return obtenirDades(userid,isManager,base);
	}
	
	public DadesPersonals obtenirDadesArrel (String userid){
				return obtenirDades(userid,false,baseRoot);
	}
	public DadesPersonals obtenirDadesAlumne (String userid){
			return obtenirDades (userid,false,baseAlu);
	}
	
	public DadesPersonals obtenirDadesAlumneAsAdmin (String userid){
		return obtenirDades (userid,true,baseAlu);
}
	
	public DadesPersonals obtenirDades(String userid,boolean isManager,String basep){

		// No vàlid
		if (userid == null)
			return null;

		try {
			NamingEnumeration enumer =null;
			if (!isManager)
				enumer = ldapUdlUtils.searchLDAP("(uid=" + userid + ")", basep);
			else
				enumer = ldapUdlUtils.searchLDAPManager("(uid=" + userid + ")", basep);
			if (enumer.hasMore()){
				DadesPersonals d = transformaResultat((SearchResult)  enumer.next());
				return d;
			}else{
				return null;
			}
			
			}catch (Exception ex) {
			System.out.println("Exception");
			return null;
			}
	}
	
	public DadesPersonals obtenirDades(String userid){
		return obtenirDades(userid,false);
	}
	

	public List getLlistaUsuaris(String cerca, boolean include) {
		List llistaUsuaris = new ArrayList();
		
		String filtre = "(&(objectclass=posixAccount)(gecos=*"+cerca+"*))";
		
		try {
			String basep;
			
			if (!include){
				basep = this.base;
			}else{
				basep = this.baseRoot;
			}

			if (cerca.length() >0){
				//System.out.println("La base es" + basep);
				NamingEnumeration enumer = ldapUdlUtils.searchLDAP(filtre,basep);
			
				while (enumer.hasMore()){
					//System.out.println("Existeixen tios");
					DadesPersonals d = transformaResultat((SearchResult) enumer.next());
					llistaUsuaris.add(d);
				}
			}
			
			}catch (Exception ex) {
				System.out.println("Exception");
			return null;
			}
		
		return llistaUsuaris;
	}

	public boolean hasThisPassword(String userId, String oldpasswd) {
		return hasThisPassword (userId,oldpasswd,base);
	}
	
	public boolean hasThisPasswordAlu(String userId, String oldpasswd) {
		return hasThisPassword (userId,oldpasswd,baseAlu);
	}
		
	public boolean hasThisPassword(String userId, String oldpasswd,String basetypr) {
		if (userId == null)
			return false;
		if (oldpasswd == null)
			return false;

		if (ldapUdlUtils == null) {
			System.out.println("LdapUtils no inicialitzat");
		}

		return ldapUdlUtils.authenticateUser(userId, oldpasswd, basetypr);
	}

	private boolean validatePasswd(String pass) {
	
		if (pass.equals("")) {
			return false;
		}
		if (pass.length() < 3 || pass.length() > 20) {
			return false;
		}
		return true;
	}

	private boolean validateDades(DadesPersonals p) {
		if (p.getNom() != null && p.getCognoms() != null && p.getTlf() != null
				&& p.getWebpersonal() != null
				&& p.getUbicacio() != null && p.getCorreuprincipal() != null) {
				
			return true;
		} else {
			return false;
		}
	}

	private DadesPersonals transformaResultat(SearchResult result){
		DadesPersonals d = new DadesPersonals();
		
		Attributes attrs= result.getAttributes();
		
		if (result!=null){
		
			// Provem a capturar els mètodes
			String dni,uid,nom,cognoms,employeetype,tlf,mobile,fax,webpersonal,ubicacio,correuprincipal,reenviament,codi, missatgeria,correualternatiu;
			boolean guardacorreu;
			
			try{
				uid=attrs.get("uid").get().toString();
			}
			catch (Exception ex){
				uid = "";
			}
			
			try{
				nom = attrs.get("givenName").get().toString();
			}catch (Exception ex){
				nom = "";
			}
			
			try{
				cognoms = attrs.get("sn").get().toString();
			}catch (Exception ex){
				cognoms= "";
			}
			
			try{
				tlf = attrs.get("telephoneNumber").get().toString();
			}catch (Exception ex){
				tlf="";
			}
			
			try{
				mobile = attrs.get("mobile").get().toString();
			}catch (Exception ex){
				mobile="";
			}
			
			try{
				fax = attrs.get("facsimileTelephoneNumber").get().toString();
			}catch (Exception ex){
				fax="";
			}
			
			try{
				webpersonal = attrs.get("registeredAddress").get().toString();
			}catch (Exception ex){
				webpersonal="";
			}
			
			try{
				employeetype = attrs.get("employeeType").get().toString();
			}catch (Exception ex){
				employeetype="";
			}

			try{
				ubicacio = attrs.get("roomNumber").get().toString();
			}catch (Exception ex){
				ubicacio="";
			}
			
			try{
				correuprincipal = attrs.get("mail").get().toString();
			}catch (Exception ex){
				correuprincipal = "";
			}
			
			try{
				correualternatiu = attrs.get("mailMessageStore").get().toString();
			}catch (Exception ex){
				correualternatiu = "";
			}
			
			//Lògica per a obtenir el password i si ja està activat
			try{
				codi = new String((byte[]) attrs.get("userPassword").get(), "UTF-8");
				
				if ("{crypt}*".equals(codi)){ //Cas de alumnes
					codi="pendentAlu";
				} else if (codi.startsWith("{crypt}")){
					codi="activat";
				}
				else{
					 codi = attrs.get("telexNumber").get().toString();
				}
				
				
			}catch (Exception exe){
				//System.out.println("No s'ha pogut obtenir el password");
				try{ 
					codi = attrs.get("telexNumber").get().toString();
					if (codi.equals("")){
						codi= "activat";
					}
				}catch (Exception ex){
					codi = "activat";	
				}
				
			}
			
			try{
				dni= attrs.get("employeeNumber").get().toString().toLowerCase();
			}catch (Exception ex){
				dni = "";
			}
			
			//Camp missatgeria instantània
			try{
				missatgeria = attrs.get("pager").get().toString();
			}catch (Exception ex){
				missatgeria = "";
			}
			
			try{
				Attribute mailForward = attrs.get("mailForwardingAddress");
				int size = mailForward.size();
				reenviament = "";
				guardacorreu = false;
				
				
				if (size >= 2){
					reenviament = mailForward.get(0).toString();
					guardacorreu=true;
					if (reenviament.equals(correuprincipal)){
						reenviament = mailForward.get(1).toString();
						guardacorreu=true;
					}
				}
				
				if (size==1){
					reenviament = mailForward.get().toString();
					guardacorreu=false;
				}
				
				if (size<1 || reenviament.equals(correuprincipal)){
					reenviament = "";
					guardacorreu = false;
				}
				
				
			}catch (Exception ex){
				reenviament = "";
				guardacorreu = false;
			}
			
			d.setUid(uid);
			d.setNom(nom);
			d.setCognoms(cognoms);
			d.setTlf(tlf);
			d.setFax(fax);
			d.setMobile(mobile);
			d.setWebpersonal(webpersonal);
			d.setEmployeeType(employeetype);
			d.setUbicacio(ubicacio);
			d.setCorreuprincipal(correuprincipal);
			d.setReenviament(reenviament);
			d.setGuardacorreu(guardacorreu);
			d.setCodi(codi);
			d.setDni(dni);
			d.setCorreuAlternatiu (correualternatiu);
			d.setMissatgeria(missatgeria);
			
			return d;
		}
		else{
			return null;
		}
	}

	//Metode que busca a tot l'LDAP	si hi ha un àlies igual
	public boolean validaUnicAliesLDAP(String aliesMissatgeria){
	
		boolean identificador=false;
	    
		Hashtable env = new Hashtable();
	        try{
	               NamingEnumeration results = ldapUdlUtils.searchLDAP("(pager="+aliesMissatgeria+")", baseRoot);
	                
	               if (results.hasMore()==false)	 identificador=true; 	//el pager es unic
				   else								 identificador=false;	//reporto un error
	              		
					results.close();
	        }
	        catch(Exception ex){											//en cas de que falli la cerca
	                ex.printStackTrace();
	        }
		return identificador;
	}
	
	private HttpClient authenticate() throws Exception {
		HttpClient httpclient = new HttpClient();
		
		
		System.out.println  ("Activació Alu: Sol·licitat autenticacio per l'activacio: Url a la que autentiquem "+ aluDiscUrl);
        
        httpclient.getState().setCredentials(
         	AuthScope.ANY,
         	new UsernamePasswordCredentials(aluDiscUsername, aluDiscPassword)
        );
	       
		   PostMethod post = new PostMethod(aluDiscUrl);
		   post.setDoAuthentication( true );
		   
           try {
        	   int response = httpclient.executeMethod(post);
        	   System.out.println  ("Activació Alu: Server response is "+response);
	        }catch (Exception ex){
	        	ex.printStackTrace();
	        	httpclient = null;
	        }
           finally {
	        	 post.releaseConnection();
           }
           return httpclient;
	}
	
	private boolean send(HttpClient httpclient,String uid) throws Exception {
			
			boolean retorn = false;
			
			PostMethod postMessage = new PostMethod(aluDiscUrl);
	        
			NameValuePair[] data = {
	                new NameValuePair("uid", uid)
	              };
	        
			postMessage.setRequestBody(data);
	              
			
			try {
				if (httpclient != null) {
					int postResponseCode = httpclient.executeMethod(postMessage);
					System.out.println  ("Activació Alu: Server response is "+postResponseCode);
	
					 if (postResponseCode != 200) {
						 System.out.println  ("Activació Alu: Server response is not OK");
					   }
					   else {
						   retorn = true;
					   }
				}
				else {
					System.out.println  ("Activació Alu: httpclient is null");
				}
				
	        }catch (Exception ex){
	        	ex.printStackTrace();
	        }
           finally {
        	   postMessage.releaseConnection();
	        } 
		   return retorn;
	    }
}
