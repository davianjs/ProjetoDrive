import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    private static final int PORTA = 5000;
    private static final String DIRETORIO_ARMAZENAMENTO = "armazenamento/";
    private static final String ARQUIVO_USUARIOS = "config/usuarios.txt"; // Adicionado um diretório para o arquivo
    private static Map<String, String> usuarios = new HashMap<>();

    public static void main(String[] args) {
        carregarUsuarios();
        try (ServerSocket servidorSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor iniciado na porta " + PORTA);
            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                System.out.println("Novo cliente conectado: " + clienteSocket);
                new GerenciadorCliente(clienteSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void carregarUsuarios() {
        File arquivoUsuarios = new File(ARQUIVO_USUARIOS);
        // Cria o diretório pai, se não existir
        File diretorioPai = arquivoUsuarios.getParentFile();
        if (diretorioPai != null && !diretorioPai.exists()) {
            diretorioPai.mkdirs();
        }

        // Tenta carregar os usuários do arquivo
        try (BufferedReader leitor = new BufferedReader(new FileReader(arquivoUsuarios))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String[] partes = linha.split(":");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Arquivo de usuários não encontrado. Criando novo...");
            try {
                arquivoUsuarios.createNewFile(); // Cria o arquivo, se não existir
            } catch (IOException ex) {
                System.out.println("Erro ao criar arquivo de usuários: " + ex.getMessage());
            }
        }
    }

    private static void salvarUsuario(String usuario, String senha) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARQUIVO_USUARIOS, true))) {
            escritor.write(usuario + ":" + senha);
            escritor.newLine();
        }
        usuarios.put(usuario, senha);
    }

    private static class GerenciadorCliente extends Thread {
        private Socket socket;
        private DataInputStream entrada;
        private DataOutputStream saida;
        private String usuarioLogado;

        public GerenciadorCliente(Socket socket) {
            this.socket = socket;
            try {
                entrada = new DataInputStream(socket.getInputStream());
                saida = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String comando = entrada.readUTF();
                    switch (comando) {
                        case "LOGIN":
                            if (!autenticar()) {
                                socket.close();
                                return;
                            }
                            break;
                        case "CADASTRO":
                            cadastrarUsuario();
                            break;
                        case "LISTAR":
                            listarArquivos();
                            break;
                        case "DOWNLOAD":
                            baixarArquivo();
                            break;
                        case "UPLOAD":
                            enviarArquivo();
                            break;
                        case "SAIR":
                            socket.close();
                            return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean autenticar() throws IOException {
            String nomeUsuario = entrada.readUTF();
            String senha = entrada.readUTF();
            if (usuarios.containsKey(nomeUsuario) && usuarios.get(nomeUsuario).equals(senha)) {
                usuarioLogado = nomeUsuario;
                criarDiretoriosUsuario();
                saida.writeUTF("LOGIN_SUCESSO");
                return true;
            } else {
                saida.writeUTF("LOGIN_FALHOU");
                return false;
            }
        }

        private void cadastrarUsuario() throws IOException {
            String nomeUsuario = entrada.readUTF();
            String senha = entrada.readUTF();
            if (usuarios.containsKey(nomeUsuario)) {
                saida.writeUTF("USUARIO_JA_EXISTE");
            } else {
                salvarUsuario(nomeUsuario, senha);
                saida.writeUTF("CADASTRO_SUCESSO");
            }
        }

        private void criarDiretoriosUsuario() {
            String diretorioUsuario = DIRETORIO_ARMAZENAMENTO + usuarioLogado;
            new File(diretorioUsuario + "/pdf").mkdirs();
            new File(diretorioUsuario + "/jpg").mkdirs();
            new File(diretorioUsuario + "/txt").mkdirs();
        }

        private void listarArquivos() throws IOException {
            String diretorioUsuario = DIRETORIO_ARMAZENAMENTO + usuarioLogado;
            File[] diretorios = new File(diretorioUsuario).listFiles(File::isDirectory);
            StringBuilder listaArquivos = new StringBuilder();

            for (File diretorio : diretorios) {
                File[] arquivos = diretorio.listFiles();
                if (arquivos != null) {
                    for (File arquivo : arquivos) {
                        listaArquivos.append(diretorio.getName()).append("/").append(arquivo.getName()).append("\n");
                    }
                }
            }
            saida.writeUTF(listaArquivos.toString());
        }

        private void baixarArquivo() throws IOException {
            String caminhoArquivo = entrada.readUTF();
            File arquivo = new File(DIRETORIO_ARMAZENAMENTO + usuarioLogado + "/" + caminhoArquivo);
            if (arquivo.exists()) {
                saida.writeUTF("ARQUIVO_ENCONTRADO");
                saida.writeLong(arquivo.length());
                Files.copy(arquivo.toPath(), saida);
            } else {
                saida.writeUTF("ARQUIVO_NAO_ENCONTRADO");
            }
        }

        private void enviarArquivo() throws IOException {
            String nomeArquivo = entrada.readUTF();
            String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf(".") + 1).toLowerCase();
            String diretorioTipo;

            switch (extensao) {
                case "pdf":
                    diretorioTipo = "pdf";
                    break;
                case "jpg":
                    diretorioTipo = "jpg";
                    break;
                case "txt":
                    diretorioTipo = "txt";
                    break;
                default:
                    saida.writeUTF("TIPO_INVALIDO");
                    return;
            }

            saida.writeUTF("TIPO_VALIDO");
            long tamanhoArquivo = entrada.readLong();
            File arquivo = new File(DIRETORIO_ARMAZENAMENTO + usuarioLogado + "/" + diretorioTipo + "/" + nomeArquivo);
            Files.copy(entrada, arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}