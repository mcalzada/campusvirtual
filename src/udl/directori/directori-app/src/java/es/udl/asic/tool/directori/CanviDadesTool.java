package es.udl.asic.tool.directori;

import es.udl.asic.api.app.directori.DirectoryService;
import es.udl.asic.api.app.directori.DadesPersonals;
import org.sakaiproject.tool.api.Session; 
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.event.cover.UsageSessionService;
import org.sakaiproject.event.api.UsageSession;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.faces.context.FacesContext;
import javax.faces.component.UIViewRoot;
import javax.faces.model.SelectItem;


public class CanviDadesTool{

	private static int NO_BLOQUEJAT=0;
	private static int BLOQUEJAT =1;
	
	
	private String errorMsg;
	private DadesPersonals dadesActuals;
	private DadesPersonals novesDades;
	private ResourceBundle messageBundle;
	private List llistaCorreus=null;
	
	private boolean desplegat = false;
	private Pattern r;

	private Pattern rcorreu;
	private Pattern rMissatgeria;
	private boolean pdipas = false;
	
	
	private DirectoryService directoryService;
	
	private String userId;
	
	int estatMissatgeria=0;
	
	public void setDirectoryService(DirectoryService ds){
		directoryService = ds;
		init();
	}
	
	public DirectoryService getDirectoryService(){
		return directoryService;
	}

	public DadesPersonals getDadesActuals(){
		return dadesActuals;
	}

	public void setDadesActuals(DadesPersonals dadesActuals) {
		this.dadesActuals = dadesActuals;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public DadesPersonals getNovesDades() {
		if (novesDades==null)
			init();
		return novesDades;
	}
	
	public void setNovesDades(DadesPersonals novesDades) {
		this.novesDades = novesDades;
	}
	
	public boolean getDesplegat(){
		return desplegat;
	}
	
	public int getEstatMissatgeria(){
		return estatMissatgeria;
	}
	public void setEstatMissatgeria(int estatMissatgeria){
		this.estatMissatgeria=estatMissatgeria;
	}
	
	/*public List getLlistaCorreus(){
		return llistaCorreus;
	}*/
	
	public void init(){
		String bundleName = FacesContext.getCurrentInstance().getApplication().getMessageBundle();
     	messageBundle = ResourceBundle.getBundle(bundleName);
		
		Placement pla = ToolManager.getCurrentPlacement();
		Properties lst = new Properties();
		
		lst.setProperty("ldapserver_ro",pla.getConfig().getProperty("ldapserver_ro"));
		lst.setProperty("ldapserver_rw",pla.getConfig().getProperty("ldapserver_rw"));
		lst.setProperty("securityPrincipal",pla.getConfig().getProperty("securityPrincipal"));
		lst.setProperty("securityCredentials",pla.getConfig().getProperty("securityCredentials"));
		lst.setProperty("base",pla.getConfig().getProperty("base"));
		lst.setProperty("baseRoot",pla.getConfig().getProperty("baseRoot"));
		lst.setProperty("baseAlu",pla.getConfig().getProperty("baseAlu"));
		
		UsageSession uses = UsageSessionService.getSession();
		userId = uses.getUserEid();
		
		//Passem els paràmetres de configuracio al servei de ldap
		try{			
			directoryService.configParameters(lst);
		}catch (Exception ex){
			System.out.println("Excepció a l'inicialitzar el servei de ldap");
		}
		
		//Obtenim les dades de l'usuari

		if (obtenirDades()){
			onReiniciar();
			
		}else{
			errorMsg = messageBundle.getString("dadeserr1");
			//Hauriam de fer la redireccio
		}
		
		//Iniciem les expresió regular per a fer la validació
		String expresio = "(([0-9])|\\+|\\ )*";
		r= Pattern.compile(expresio); // Patró (Expresió regular)
		
		String expresiocorreu = ".+@.*\\.([a-z]|[A-Z])*";
		rcorreu = Pattern.compile(expresiocorreu);
	
		//Patró per a la missatgeria instantània
		String expressioMissatgeria = "[a-zA-Z0-9.-]?[a-zA-Z0-9.-]*";
		rMissatgeria = Pattern.compile(expressioMissatgeria);

	}
		
	public String onDesar(){
		if (validaDades()){
			String aliesMissatgeria = "";
			aliesMissatgeria = novesDades.getMissatgeria();
			
			if (getEstatMissatgeria()==0){ 
				if (directoryService.validaUnicAliesLDAP(aliesMissatgeria)){
					//Correcte el pager de LDAP és únic
					return "revisadades";
				}
				else{
					//Hi ha una entrada igual a LDAP per aquest Alies
					errorMsg = messageBundle.getString("dadeserr10");
					return "errordades";
				}
			}
			else
					return "revisadades";
		}
		else{
			return "errordades";
		}
	}
	
	public String onDesarAlu(){
		if (validaDadesAlumne()){
			return "revisadadesalu";
		}else{
			return "errordadesalu";
		}
	}
	

	
	public String onReiniciar(){
		if (novesDades==null){
			novesDades = new DadesPersonals();
		}
		//Afegim totes les dades velles a les noves
		novesDades.setNom(dadesActuals.getNom());
		novesDades.setCognoms(dadesActuals.getCognoms());
		novesDades.setTlf(dadesActuals.getTlf());
		novesDades.setMobile(dadesActuals.getMobile());
		novesDades.setFax(dadesActuals.getFax());
		novesDades.setWebpersonal(dadesActuals.getWebpersonal());
		novesDades.setUbicacio(dadesActuals.getUbicacio());
		novesDades.setCorreuprincipal (dadesActuals.getCorreuprincipal());
		novesDades.setCorreuAlternatiu (dadesActuals.getCorreuAlternatiu());
		novesDades.setReenviament(dadesActuals.getReenviament());
		novesDades.setGuardacorreu(dadesActuals.getGuardacorreu());
		//Busco el camp missatgeria
		novesDades.setMobile(dadesActuals.getMobile());
		novesDades.setMissatgeria(dadesActuals.getMissatgeria());
		if(dadesActuals.getMissatgeria() != "") setEstatMissatgeria(BLOQUEJAT); 
		else 									setEstatMissatgeria(NO_BLOQUEJAT);
		
		return "canvidades";
	}
	
	public String onReiniciarAlu(){
		if (novesDades==null){
			novesDades = new DadesPersonals();
		}
		//Afegim totes les dades velles a les noves
		novesDades.setNom(dadesActuals.getNom());
		novesDades.setCognoms(dadesActuals.getCognoms());
		novesDades.setCorreuprincipal (dadesActuals.getCorreuprincipal());
		novesDades.setCorreuAlternatiu (dadesActuals.getCorreuAlternatiu());
		novesDades.setReenviament(dadesActuals.getReenviament());
		
		return "canvidadesalu";
	}

	
	
	public String onCancelar(){
		return "canvidades";
	}
	
	public String onCancelarAlu(){
		return "canvidadesalu";
	}
	

	public String onFinalitzar(){
		boolean canvidades = directoryService.canviDades(userId,novesDades);
		
		if (canvidades){
			//Actualitzo el estatMissatgeria per a bloquejar la missatgeria
			if(novesDades.getMissatgeria() != "") 	setEstatMissatgeria(BLOQUEJAT); 
			else 									setEstatMissatgeria(NO_BLOQUEJAT);
		
			return "correctedades";
		}
		else{
			errorMsg = messageBundle.getString("dadeserr2");
			return "errordades";
		}
	}
	
	
	public String onFinalitzarAlu(){
		boolean canvidades = directoryService.canviDadesAlumne(userId,novesDades);

		if (canvidades){
			return "correctedadesalu";
		}
		else{
			errorMsg = messageBundle.getString("dadeserr12");
			return "errordadesalu";
		}
	}
	
	public String onOkError(){
		return "canvidades";
	}
	

	
	public String onOkConfirmar(){
		return "canvidades";
	}
	
	public String onOkErrorAlu(){
		return "canvidadesalu";
	}
	
	public String onOkConfirmarAlu(){
		return "canvidadesalu";
	}

	public String desplega(){
		desplegat = !desplegat;
		return "canvidades";
	}
	
	
	private boolean validaDadesAlumne (){
		errorMsg = "";
		boolean valid=true;
		
		if (!novesDades.getCorreuAlternatiu().equals("")){
			novesDades.setCorreuAlternatiu(novesDades.getCorreuAlternatiu().trim());
			if (!rcorreu.matcher(novesDades.getCorreuAlternatiu()).matches()){
				errorMsg = messageBundle.getString ("dadeserr13");
				valid=false;
			}
		}
		
		if (!novesDades.getReenviament().equals("")){
			novesDades.setReenviament(novesDades.getReenviament().trim());
			if (!rcorreu.matcher(novesDades.getReenviament()).matches()){
				errorMsg = messageBundle.getString ("dadeserr8");
				valid=false;
			}
		}
		
		return valid;
	}
	
	private boolean validaDades(){
		errorMsg = "";
		boolean valid=true;
		
		if (novesDades.getNom().length() <1){
			errorMsg = messageBundle.getString("dadeserr3");
			valid = false;
		}
		if (novesDades.getCognoms().length() <1){
			errorMsg = messageBundle.getString("dadeserr4");
			valid = false;
		}
		
		if (!r.matcher(novesDades.getTlf()).matches()){
			errorMsg = messageBundle.getString ("dadeserr6");
			valid = false;
		}
		
		if (!r.matcher(novesDades.getFax()).matches()){
			errorMsg = messageBundle.getString ("dadeserr7");
			valid = false;
		}
		if (!novesDades.getCorreuAlternatiu().equals("")){
			novesDades.setCorreuAlternatiu(novesDades.getCorreuAlternatiu().trim());
			if (!rcorreu.matcher(novesDades.getCorreuAlternatiu()).matches()){
				errorMsg = messageBundle.getString ("dadeserr13");
				valid=false;
			}
		}
		
		if (!novesDades.getReenviament().equals("")){
			novesDades.setReenviament(novesDades.getReenviament().trim());
			if (!rcorreu.matcher(novesDades.getReenviament()).matches()){
				errorMsg = messageBundle.getString ("dadeserr8");
				valid=false;
			}
		}
		
		//Comprovo lèxicament el camp missatgeria
		if (!rMissatgeria.matcher(novesDades.getMissatgeria()).matches()){
			errorMsg = messageBundle.getString ("dadeserr9");
			valid = false;
		}
		//Si no escric rés al camp missatgeria ho deixo tal qual
		if (novesDades.getMissatgeria().equals("")){
			novesDades.setMissatgeria("");
		}
				
		if (novesDades.getTlf().equals("")){
			novesDades.setTlf("973");
		}
		if (novesDades.getFax().equals("")){
			novesDades.setFax("973");
		}
			
		
		return valid;
	}

	
	
	private boolean obtenirDades(){
		dadesActuals =  directoryService.obtenirDadesAlumne(userId);
		
		if (dadesActuals==null){
			pdipas=true;
			dadesActuals =  directoryService.obtenirDades(userId);
		}

		if (dadesActuals==null){
			
			String bundleName = FacesContext.getCurrentInstance().getApplication().getMessageBundle();
			ResourceBundle messageBundle = ResourceBundle.getBundle(bundleName);

			errorMsg = messageBundle.getString("dadeserr5");
			return false; 
		}
		
		return true;
	}
}

