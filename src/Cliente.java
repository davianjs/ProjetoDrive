import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class Cliente {
    private static final String ENDERECO_SERVIDOR = "localhost";
    private static final int PORTA_SERVIDOR = 5000;
    private static final String DIRETORIO_ARMAZENAMENTO = "armazenamento/";
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream saida;
    private JFrame janela;
    private JTabbedPane abas;
    private Map<String, JPanel> paineisPastas;
    private Map<String, JLabel> miniaturas;
    private String usuarioLogado; // Adicionado para armazenar o usuário logado

    public Cliente() {
        criarInterfaceGrafica();
    }

    private void criarInterfaceGrafica() {
        janela = new JFrame("Cliente de Arquivos");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setSize(600, 400);
        janela.setLayout(new BorderLayout(10, 10));

        mostrarTelaLogin();
        janela.setVisible(true);
    }

    private void mostrarTelaLogin() {
        JPanel painelLogin = new JPanel(new GridLayout(4, 2, 10, 10));
        painelLogin.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField campoUsuario = new JTextField();
        JPasswordField campoSenha = new JPasswordField();
        JButton botaoLogin = new JButton("Entrar");
        JButton botaoCadastro = new JButton("Cadastrar");

        painelLogin.add(new JLabel("Usuário:"));
        painelLogin.add(campoUsuario);
        painelLogin.add(new JLabel("Senha:"));
        painelLogin.add(campoSenha);
        painelLogin.add(new JLabel(""));
        painelLogin.add(botaoLogin);
        painelLogin.add(new JLabel(""));
        painelLogin.add(botaoCadastro);

        botaoLogin.addActionListener(e -> {
            try {
                socket = new Socket(ENDERECO_SERVIDOR, PORTA_SERVIDOR);
                entrada = new DataInputStream(socket.getInputStream());
                saida = new DataOutputStream(socket.getOutputStream());

                if (fazerLogin(campoUsuario.getText(), campoSenha.getText())) {
                    usuarioLogado = campoUsuario.getText(); // Armazena o usuário logado
                    janela.getContentPane().removeAll();
                    mostrarTelaPrincipal();
                    janela.revalidate();
                    janela.repaint();
                } else {
                    JOptionPane.showMessageDialog(janela, "Falha no login.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        botaoCadastro.addActionListener(e -> {
            try {
                socket = new Socket(ENDERECO_SERVIDOR, PORTA_SERVIDOR);
                entrada = new DataInputStream(socket.getInputStream());
                saida = new DataOutputStream(socket.getOutputStream());

                fazerCadastro(campoUsuario.getText(), campoSenha.getText());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        janela.add(painelLogin, BorderLayout.CENTER);
    }

    private boolean fazerLogin(String usuario, String senha) throws IOException {
        saida.writeUTF("LOGIN");
        saida.writeUTF(usuario);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        return resposta.equals("LOGIN_SUCESSO");
    }

    private void fazerCadastro(String usuario, String senha) throws IOException {
        saida.writeUTF("CADASTRO");
        saida.writeUTF(usuario);
        saida.writeUTF(senha);
        String resposta = entrada.readUTF();
        if (resposta.equals("CADASTRO_SUCESSO")) {
            JOptionPane.showMessageDialog(janela, "Cadastro realizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(janela, "Usuário já existe.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarTelaPrincipal() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(new EmptyBorder(20, 20, 20, 20));

        abas = new JTabbedPane();
        paineisPastas = new HashMap<>();
        miniaturas = new HashMap<>();

        String[] pastas = {"pdf", "jpg", "txt"};
        for (String pasta : pastas) {
            JPanel painelPasta = new JPanel(new GridLayout(0, 3, 10, 10));
            paineisPastas.put(pasta, painelPasta);
            abas.addTab(pasta.toUpperCase(), new JScrollPane(painelPasta));
        }

        JPanel painelBotoes = new JPanel(new GridLayout(1, 4, 10, 10));
        JButton botaoListar = new JButton("Listar Arquivos");
        JButton botaoDownload = new JButton("Fazer Download");
        JButton botaoUpload = new JButton("Fazer Upload");
        JButton botaoSair = new JButton("Sair");

        botaoListar.addActionListener(e -> listarArquivos());
        botaoDownload.addActionListener(e -> fazerDownload());
        botaoUpload.addActionListener(e -> fazerUpload());
        botaoSair.addActionListener(e -> {
            try {
                desconectar();
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        painelBotoes.add(botaoListar);
        painelBotoes.add(botaoDownload);
        painelBotoes.add(botaoUpload);
        painelBotoes.add(botaoSair);

        painelPrincipal.add(abas, BorderLayout.CENTER);
        painelPrincipal.add(painelBotoes, BorderLayout.SOUTH);
        janela.add(painelPrincipal, BorderLayout.CENTER);
    }

    private void listarArquivos() {
        try {
            saida.writeUTF("LISTAR");
            String lista = entrada.readUTF();
            for (String pasta : paineisPastas.keySet()) {
                paineisPastas.get(pasta).removeAll();
            }
            miniaturas.clear();

            if (usuarioLogado == null || usuarioLogado.trim().isEmpty()) {
                JOptionPane.showMessageDialog(janela, "Usuário não logado.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (String linha : lista.split("\n")) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split("/");
                String pasta = partes[0];
                String nomeArquivo = partes[1];

                JPanel painelArquivo = new JPanel(new BorderLayout());
                painelArquivo.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

                JLabel labelNome = new JLabel(nomeArquivo);
                painelArquivo.add(labelNome, BorderLayout.SOUTH);

                if (pasta.equals("jpg")) {
                    String caminhoImagem = DIRETORIO_ARMAZENAMENTO + usuarioLogado + "/" + linha;
                    File arquivoImagem = new File(caminhoImagem);
                    if (arquivoImagem.exists()) {
                        JLabel miniatura = new JLabel(new ImageIcon(new ImageIcon(caminhoImagem).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                        miniaturas.put(linha, miniatura);
                        painelArquivo.add(miniatura, BorderLayout.CENTER);
                    } else {
                        JLabel labelErro = new JLabel("Imagem não encontrada", SwingConstants.CENTER);
                        labelErro.setForeground(Color.RED);
                        painelArquivo.add(labelErro, BorderLayout.CENTER);
                    }
                } else {
                    JLabel icone = new JLabel(pasta.equals("pdf") ? "PDF" : "TXT", SwingConstants.CENTER);
                    icone.setForeground(Color.BLUE);
                    painelArquivo.add(icone, BorderLayout.CENTER);
                }

                paineisPastas.get(pasta).add(painelArquivo);
            }

            for (String pasta : paineisPastas.keySet()) {
                paineisPastas.get(pasta).revalidate();
                paineisPastas.get(pasta).repaint();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fazerDownload() {
        String caminhoArquivo = JOptionPane.showInputDialog(janela, "Digite o caminho do arquivo (ex: pdf/documento.pdf):");
        if (caminhoArquivo != null && !caminhoArquivo.trim().isEmpty()) {
            JFileChooser seletorArquivo = new JFileChooser();
            seletorArquivo.setDialogTitle("Escolha onde salvar o arquivo");
            if (seletorArquivo.showSaveDialog(janela) == JFileChooser.APPROVE_OPTION) {
                File arquivoLocal = seletorArquivo.getSelectedFile();
                try {
                    saida.writeUTF("DOWNLOAD");
                    saida.writeUTF(caminhoArquivo);
                    String resposta = entrada.readUTF();

                    if (resposta.equals("ARQUIVO_ENCONTRADO")) {
                        long tamanhoArquivo = entrada.readLong();
                        Files.copy(entrada, arquivoLocal.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        JOptionPane.showMessageDialog(janela, "Download concluído: " + arquivoLocal.getName(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(janela, "Arquivo não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fazerUpload() {
        JFileChooser seletorArquivo = new JFileChooser();
        seletorArquivo.setDialogTitle("Escolha o arquivo para upload");
        seletorArquivo.setFileFilter(new FileNameExtensionFilter("Arquivos suportados", "pdf", "jpg", "txt"));
        if (seletorArquivo.showOpenDialog(janela) == JFileChooser.APPROVE_OPTION) {
            File arquivo = seletorArquivo.getSelectedFile();
            try {
                saida.writeUTF("UPLOAD");
                saida.writeUTF(arquivo.getName());
                String resposta = entrada.readUTF();

                if (resposta.equals("TIPO_INVALIDO")) {
                    JOptionPane.showMessageDialog(janela, "Tipo de arquivo inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                saida.writeLong(arquivo.length());
                Files.copy(arquivo.toPath(), saida);
                JOptionPane.showMessageDialog(janela, "Upload concluído: " + arquivo.getName(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                listarArquivos(); // Atualiza a lista após o upload
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void desconectar() throws IOException {
        saida.writeUTF("SAIR");
        socket.close();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente());
    }
}