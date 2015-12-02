package es.fdi.iw.controller;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import es.fdi.iw.model.Actividad;
import es.fdi.iw.model.Usuario;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	@PersistenceContext
	private EntityManager entityManager;
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	
	@SuppressWarnings("unused")
	public String login(HttpServletRequest request,
	        HttpServletResponse response, 
	        Model model, HttpSession session) {
	         if (true ) {
	            session.setAttribute("user", "usuario");
	         } else {
	             
	            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	            model.addAttribute("loginError", 
	                "Te lo estás inventando!");
	         }
	         return "home";
	    }
	
	
	@RequestMapping(value = "/registro", method = RequestMethod.POST)
	@Transactional
	public String registro(
			@RequestParam("login") String formLogin,
			@RequestParam("pass") String formPass,
			@RequestParam("pass2") String formPass2,
			@RequestParam("email") String formEmail,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
		logger.info("Login attempt from '{}' while visiting '{}'", formLogin);
		
		// validate request
		if (formLogin == null || formLogin.length() < 3 || formPass == null || formPass.length() < 4) 
		{
			model.addAttribute("loginError", "usuarios y contraseñas: 4 caracteres mínimo");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} 
		else 
		{
			Usuario u = null;
			try {
				u = (Usuario)entityManager.createNamedQuery("userByLogin")
					.setParameter("loginParam", formLogin).getSingleResult();
				
			} catch (NoResultException nre) {
				if (formPass.equals(formPass2)) 
				{
					// UGLY: register new users if they do not exist and pass is 4 chars long
					logger.info("no-such-user; creating user {}", formLogin);				
					Usuario user = Usuario.createUser(formLogin, formPass, "usuario", null, "Sin especificar", formEmail);
					entityManager.persist(user);				
				} 
				else {
					logger.info("no such login: {}", formLogin);
				}
				model.addAttribute("loginError", "error en usuario o contraseña");
			}
		}
		
		// redireccion a login cuando el registro ha sido correcto
		return "redirect:login";
	}
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@Transactional
	public String login(
			@RequestParam("login") String formLogin,
			@RequestParam("pass") String formPass,
			String destino,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
		logger.info("Login attempt from '{}' while visiting '{}'", formLogin);
		destino="login";
		
		// La instruccion model.addAtribute creo que pone en el modelo (los jsp) el mensaje del seguindo parametro 
			//en el nombre de la clase que es el primer parametro
		
				if (formLogin == null || formLogin.length() < 3 || formPass == null || formPass.length() < 3) {
					model.addAttribute("loginError", "usuarios y contraseñas: 3 caracteres mínimo");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				} else {
					Usuario u = null;
					try {
						//Hay que entender que hace esta instruccion
						u = (Usuario)entityManager.createNamedQuery("userByLogin")
							.setParameter("loginParam", formLogin).getSingleResult();
						if (u.isPassValid(formPass)) {
							model.addAttribute("loginError","pass valido");
							logger.info("pass valido");
							session.setAttribute("usuario", u);
							getTokenForSession(session);
							destino="home";
						} else {
							logger.info("pass no valido");
							model.addAttribute("loginError", "error en usuario o contraseña");
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						}
					} catch (NoResultException nre) {
						
						model.addAttribute("loginError", "error en usuario o contraseña");
					}
				}

				return "redirect:" + destino;
			}
	
	/**
	 * Logout. Elimina la sesion actual y cierra sesion redirigiendo a la pantalla de login.
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpSession session) {
		logger.info("User '{}' logged out", session.getAttribute("usuario"));
		session.invalidate();
		return "redirect:login";
	}
	
	
	/*
	 *	Metodo donde se pueden modificar los datos del usuario una vez esta logueado.
	 *	Además se puede agregar informacion como provincia, fecha de nacimiento etc.
	 */
	
	@SuppressWarnings("unused")
	@RequestMapping(value = "/mi_perfil", method = RequestMethod.POST)
	@Transactional
	public String modPerfil(
			@RequestParam("nick_perfil") String nick,
			@RequestParam("prov_perfil") String provincia,
			@RequestParam("email_perfil") String email,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
			Usuario u = null;
			try {
								
				u = (Usuario)entityManager.createNamedQuery("userByLogin")
					.setParameter("loginParam", nick).getSingleResult();
				
				u.setProvincia(provincia);
				u.setMail(email);
				
				entityManager.merge(u);
				
				
			} catch (NoResultException nre) {
		
					//Error
			}
		
		// redireccion a login cuando el registro ha sido correcto
		return "redirect:mi_perfil";
	}

	@RequestMapping(value = "/crearActividad", method = RequestMethod.POST)
	@Transactional
	public String crearActividad(
			@RequestParam("nombre_actv") String nombre_actv,
			@RequestParam("max_participantes") int max_participantes,
			//@RequestParam("fecha_ini") Date fecha_ini,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {

			Actividad a = null;
			Usuario u = null;
			try {
				u = (Usuario)entityManager.createNamedQuery("userByLogin")
						.setParameter("loginParam", session.getAttribute("usuario")).getSingleResult();
				
				a = Actividad.crearActividad(nombre_actv,max_participantes,u);
				
				entityManager.persist(a);
				
				
			} catch (NoResultException nre) {
	
				//Error
		}

				return "redirect:mis_actividades";
			}


	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		
		return "home";
	}
	
	
	@RequestMapping(value = "home", method = RequestMethod.GET)
	public String home(){
		
		return "home";
	}
	
	
	@RequestMapping(value = "/crear", method = RequestMethod.GET)
	public String crear(){
		return "crear";
	}
	
	@RequestMapping(value = "/mis_actividades", method = RequestMethod.GET)
	public String mis_actividades(Model model){
		model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
		return "mis_actividades";
	}
	
	@RequestMapping(value = "/buscar", method = RequestMethod.GET)
	public String buscar(Model model){
		model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
		
		return "buscar";
	}
	
	@RequestMapping(value = "/actividad", method = RequestMethod.GET)
	public String actividad(Model model){
		model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
		
		return "actividad";
	}
	@RequestMapping(value = "/actividad_creador", method = RequestMethod.GET)
	public String actividad_creador(){
		return "actividad_creador";
	}
	
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(){
		return "login";
	}
	
	@RequestMapping(value = "/perfil", method = RequestMethod.GET)
	public String perfil(){
		return "perfil";
	}
	
	@RequestMapping(value = "/mi_perfil", method = RequestMethod.GET)
	public String mi_perfil(){
		return "mi_perfil";
	}
	
	@RequestMapping(value = "/mensajes", method = RequestMethod.GET)
	public String mensajes(){
		return "mensajes";
	}
	
	@RequestMapping(value = "/registro", method = RequestMethod.GET)
	public String registro(){
		return "registro";
	}
	
	@RequestMapping(value = "/sin_registro", method = RequestMethod.GET)
	public String sin_registro(){
		return "sin_registro";
	}
	
	@RequestMapping(value = "/administrador", method = RequestMethod.GET)
	@Transactional
	public String administrador(Model model){
		model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
		model.addAttribute("mensajes", entityManager.createNamedQuery("allMensajes").getResultList());
		model.addAttribute("usuarios", entityManager.createNamedQuery("allUsers").getResultList());
		model.addAttribute("tags", entityManager.createNamedQuery("allTags").getResultList());
		model.addAttribute("novedades", entityManager.createNamedQuery("allNovedades").getResultList());
		model.addAttribute("pagos", entityManager.createNamedQuery("allPagos").getResultList());
		model.addAttribute("hitos", entityManager.createNamedQuery("allHitos").getResultList());
		model.addAttribute("comentarios", entityManager.createNamedQuery("allComentarios").getResultList());
		model.addAttribute("foros", entityManager.createNamedQuery("allForos").getResultList());

		return "administrador";
	}
	
	
	static String getTokenForSession (HttpSession session) {
	    String token=UUID.randomUUID().toString();
	    session.setAttribute("csrf_token", token);
	    return token;
	}
	
}
