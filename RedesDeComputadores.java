import java.io.*; //Biblioteca de entrada e saida de dados
import java.net.*; //Biblioteca que trabalho com os protocolos da internet
import java.util.*; 
import java.text.SimpleDateFormat; 
import java.nio.file.*;

public final class WebServer {
	//A porta é especificada
	private final static int PORT =  8080;
	private final static String SERVERSTRING = "Server: Jastonex/0.1";
	//Mapa de Hash de cada tipo de arquivo
	private static final Map<String, String> mimeMap = new HashMap<String, String>() {{
		put("html", "text/html");
		put("css", "text/css");
		put("js", "application/js");
		put("jpg", "image/jpg");
		put("jpeg", "image/jpeg");
		put("png", "image/png");
	}};
	//Cabeçalho de resposta é enviado
	private static void respondHeader(String code, String mime, int length, DataOutputStream out) throws Exception {
		System.out.println(" (" + code + ") ");
		out.writeBytes("HTTP/1.0 " + code + " OK\r\n");
		out.writeBytes("Content-Type: " + mimeMap.get(mime) + "\r\n");
		out.writeBytes("Content-Length: " + length + "\r\n"); 
		out.writeBytes(SERVERSTRING);
		out.writeBytes("\r\n\r\n");
	}

	private static void respondContent(String inString, DataOutputStream out) throws Exception {
		String method = inString.substring(0, inString.indexOf("/")-1);
		String file = inString.substring(inString.indexOf("/")+1, inString.lastIndexOf("/")-5);
		// Coloca index.html como arquivo padrão
		if(file.equals("") || file.isEmpty())
			file = "index.html";	
		String mime = file.substring(file.indexOf(".")+1);		

		// Retorna uma mensagem de conexão terminada, caso seja inserido um caractere fora do comum
		if(file. contains(";") || file.contains("*"))	{
			System.out.println(" (Dropping connection)");
			return;
		}
		//É feita a requesição de um arquivo e o método GET, caso o arquivo exista ele manda uma mensagem com o código 200, caso contrário ele envia o código
		//404, que indica um erro de arquivo não encontrado
		if(method.equals("GET")) {
			try {
				// Abre o arquivo
				byte[] fileBytes = null;
				InputStream is = new FileInputStream(file);
				fileBytes = new byte[is.available()];
				is.read(fileBytes);
	
				// Envia cabeçalho de arquivo encontrado
				respondHeader("200", mime, fileBytes.length, out);
				
				// Escreve o conteudo do arquivo
				out.write(fileBytes);
			
			} catch(FileNotFoundException e) {
				// Caso arquivo não seja encontrado é enviado uma mensagem de "Erro 404 File Not Found"
				try {
					byte[] fileBytes = null;
					InputStream is = new FileInputStream("404.html");
					fileBytes = new byte[is.available()];
					is.read(fileBytes);
					respondHeader("404", "html", fileBytes.length, out);
					out.write(fileBytes);
				} catch(FileNotFoundException e2) {
					String responseString = "404 File Not Found";
					respondHeader("404", "html", responseString.length(), out);
					out.write(responseString.getBytes());
				}
			}
		} else if(method.equals("POST")) {
		//Cabeçalhos
		} else if(method.equals("HEAD")) {
			respondHeader("200", "html", 0, out);
		} else {
			respondHeader("501", "html", 0, out);
		}
	}
	//"Método construtor"
	private static class WorkerRunnable implements Runnable {
		//Cria socket
		protected Socket socket = null;
		//Criar o buffer, dataoutputstream e uma string
		BufferedReader in;
		DataOutputStream out;
		String inString;
		//Lê a requisição de um socket
		public WorkerRunnable(Socket connectionSocket) throws Exception {
			//Conecta o socket criado, com o socket de conexão
			this.socket = connectionSocket;
			this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.out = new DataOutputStream(this.socket.getOutputStream());

			this.inString = this.in.readLine();

			Calendar cal = Calendar.getInstance();
			cal.getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); //Pega a hora da requisição
			String time = "[" + sdf.format(cal.getTime()) + "] "; //Formata para o padrão de hora
			System.out.print(time + this.socket.getInetAddress().toString() + " " + this.inString); //Exibe o IP de quem fez a requisição			
		}

		public void run() {
			try{
				if(this.inString != null)
					respondContent(this.inString, this.out);

				this.out.flush();
				this.out.close();
				this.in.close();

			} catch (Exception e) { 
				System.out.println("Error flushing and closing");				
			}
		}
	}
	//Método principal
	public static void main(String argv[]) throws Exception {
		//Criação do socket com a porta definida
		ServerSocket serverSocket = new ServerSocket(PORT);
		//Criar um loop infinito, para o server sempre aceitar as requisições
		for(;;) {
			//Aceita a conexão de um socket
			Socket connectionSocket = serverSocket.accept();
			//Criar um novo thread, para começar a buscar as requisições
			new Thread(new WorkerRunnable(connectionSocket)).start();	
		}
	}
}
