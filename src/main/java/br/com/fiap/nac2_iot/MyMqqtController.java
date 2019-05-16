package br.com.fiap.nac2_iot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@Path("iot/turma/4sis/grupo/nostres/devtype/arduino")
public class MyMqqtController {
	private IMqttClient mqttClient;
	private String lastMessage = null;

	public MyMqqtController() throws MqttException {
		try {
			String url = "tcp://iot.eclipse.org:1883";
			String clientId = UUID.randomUUID().toString();
			// Por padrão o Paho usa persistência em disco,
			// mas pode ter problema com permissão quando usado em um Webservice
			MqttClientPersistence persist = new MemoryPersistence();
			mqttClient = new MqttClient(url, clientId, persist);

			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(10);

			mqttClient.connect(options);
		} catch (MqttException e) {
			throw new RuntimeException("Não foi possível se conectar ao Mqqt");
		}
	}

	// Recupera uma lista de IDs de dispositivos do tipo especificado, no formato
	// JSON
	@GET
	@Path("devid/sensor/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<Integer, String> getListAllDispositivos() {
		Map<Integer, String> dispositivos = new HashMap<Integer, String>();
		dispositivos.put(1, "Arduino");
		return dispositivos;
	}

	// Recupera a lista dos sensores disponíveis para o dispositivo, no formato JSON
	@GET
	@Path("{id}/sensor/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Integer> getListAllSensores(@PathParam("id") Integer idDispositivo) throws RuntimeException {
		if (idDispositivo.equals(1009)) {
			Map<String, Integer> sensores = new HashMap<String, Integer>();
			sensores.put("Fotorresistor", 1);
			sensores.put("DHT", 1);

			return sensores;
		} else {
			throw new RuntimeException("Dispositivo não encontrado");
		}
	}

	// Recupera o último valor lido no sensor, no formato JSON {"value": <VALOR>}
	@GET
	@Path("{id}/sensor/{sensor}")
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized String getLastValueBySensor(@PathParam("id") Integer idDispositivo,
			@PathParam("sensor") String sensor) throws MqttSecurityException, MqttException, InterruptedException {

		lastMessage = null;
		mqttClient.subscribe(getTopicoSensor(sensor, idDispositivo), new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				synchronized (MyMqqtController.this) {
					lastMessage = message.toString();
					MyMqqtController.this.notifyAll();
				}
			}
		});
		this.wait();
		return lastMessage;
	}

	// Recupera a lista dos comandos disponíveis para o dispositivo, no formato JSON
	@GET
	@Path("{id}/cmd/all")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getListAllCommands() {

		ArrayList<String> listaComandos = new ArrayList<String>();
		listaComandos.add("led1");
		listaComandos.add("led2");
		return listaComandos;
	}

	// Executa um comando, passando um argumento da forma {"value": <VALOR>}
	@POST
	@Path("{id}/cmd/{cmd}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String postCommand(@PathParam("id") Integer idDispositivo, @PathParam("cmd") String comando, String message)
			throws MqttPersistenceException, MqttException {
		 
//		return getTopicoCmd(comando, idDispositivo);
		if (mqttClient.isConnected()) {
			MqttMessage msg = new MqttMessage(message.getBytes());
			msg.setQos(0);
			msg.setRetained(false);

			mqttClient.publish(getTopicoCmd(comando, idDispositivo), msg);
			return "Mensagem enviada para o tópico: " + getTopicoCmd(comando, idDispositivo);
		} else
			return "MqttCliente não conectado";
	}

	public String getTopicoSensor(String sensor, Integer idDispositivo) {
		String topico = "fiap/iot/turma/4sis/grupo/nostres/devtype/arduino/devid/" + idDispositivo + "/sensor/"
				+ sensor;
		return topico;
	}

	public String getTopicoCmd(String cmd, Integer idDispositivo) {
		String topico = "fiap/iot/turma/" + "4sis/grupo/nostres/devtype/" + "arduino/devid/" + idDispositivo + "/cmd/"
				+ cmd;
		return topico;
	}

}
