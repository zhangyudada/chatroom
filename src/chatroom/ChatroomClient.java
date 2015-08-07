package chatroom;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author zhangyu
 */
public class ChatroomClient {
    
    Selector selector                           = null;                         //ѡ������ȫ�ֵ�
    SocketChannel socketChannel                 = null;                         //��������ͨ��
    private final int port                      = 12345;                        //�������˵Ķ˿�
    private final Charset charset               = Charset.forName("UTF-8");     //ͳһ����UTF-8(Unicode)����ͽ��룬�����������
    //������ʵ�ֵ�List�������Ԫ�ؽ��п���������ʣ�������String���͵ı�����ʷ��Ϣ
    private ArrayList historyArrayList          = new ArrayList();
    
    public void initClient() throws IOException{
        //������������������ӣ���������û�п��������׳�java.net.ConnectException�쳣��������쳣����
        try{
            selector = Selector.open();                                         //�½�ѡ�������󣬿��Ƹ���channel
            //���ݷ��������׽������ӷ�������127.0.0.1Ϊ���ز��Ե�ַ��port�Ƿ������˿�
            socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",port));

            socketChannel.configureBlocking(false);                             //��Selector��ͬ����ʱ��������Ϊ����������ģʽ       
            //��socketChannelע�ᵽselector��ע���¼�Ϊ����������
            //����һ��SelectionKey��Ķ�������û�б�Ҫʹ��������󣬹�û�ж�������
            //���������ϵ��socketChannel��selector
            socketChannel.register(selector, SelectionKey.OP_READ);
            //����һ�����̣߳���ȡ���������͹�������Ϣ
            new Thread(new clientRunnable()).start();  
            //��ȡ����̨��������Ϣ
            scanConsole(socketChannel); 
        }catch(ConnectException e){
            System.out.println("������δ������3����Զ��رտͻ���");
            //��ǰ�߳���ͣ3���رգ�
            try{
                Thread thread = Thread.currentThread();
                thread.sleep(3000);//��ͣ3���������ִ��
                System.exit(0);
            }catch (InterruptedException  ex) {}
        }
               
    }
    
    //�ӿ���̨ɨ�裬����ȡ����̨�������Ϣ��������ش���
    void scanConsole(SocketChannel socketChannel) throws IOException{
        //����һ��Scanner������̨��һֱ�ȴ����룬ֱ���ûس���������������������ݴ���Scanner����Ϊɨ�����
        Scanner scanner = new Scanner(System.in);
        //����ڴ�ɨ�����������д�����һ�У��򷵻�true       
        while(scanner.hasNextLine()){
            //���Ҳ��������Դ�ɨ��������һ���������
            String msg = scanner.nextLine();
            
            //����������Ϣ��������̨ʲôҲû����ֱ�Ӿͻس��ˣ�
            if(msg.length() == 0)
                continue;
            //��Ϣ��Ϊ�գ�����Ϣ�����ǿո��Ʊ���Ȳ�����ʾ���ַ��ķ��ţ�
            else{
                //��ͼ��msg�Կո�ָ��ٴ浽����msgArray��
                //�������̨����������ɸ��ո���msgArray��ʲô�������
                //��ʱ������msgArray[0]�ͻ��׳������±�Խ���쳣ArrayIndexOutOfBoundsException
                try{
                    //��msg�Կո�ָ��ٴ浽����msgArray��
                    String[] msgArray = msg.split(" ");
                    switch(msgArray[0]){
                        //��ѯ������ʷ��Ϣ��¼�����������������Ϣ������Ҫ�����쳣����
                        case "/history": history(msgArray);break;
                        //�ͻ���Ҫ���˳������Ȱ���Ϣ�������������������پ�֪���ͻ������˳��ˣ�Ȼ��㲥�ͻ����˳���Ϣ��
                        //����"/quit"������������ַ�����������֮��ֻ��"/quit"����������
                        //���ſͻ��˹رճ���
                        case "/quit"   :
                            //����������ر��ˣ����������������Ϣ���׳�java.io.IOException�쳣
                            //��Ҫ�����쳣����
                            try{
                                socketChannel.write(charset.encode("/quit"));
                                System.exit(0);
                                break;
                            }catch(IOException e){
                                System.out.println("�������Ѿ��رգ�3����Զ��رտͻ���");
                                //��ǰ�߳���ͣ3���رգ�
                                try{
                                    Thread thread = Thread.currentThread();
                                    thread.sleep(3000);//��ͣ3���������ִ��
                                    System.exit(0);
                                }catch (InterruptedException  ex) {}
                            }
                        
                        //���������ֱ�ӽ���Ϣ���͵����������ɷ���������
                        default        : 
                            //����������ر��ˣ����������������Ϣ���׳�java.io.IOException�쳣
                            //��Ҫ�����쳣����
                            try{
                                socketChannel.write(charset.encode(msg));
                            }catch(IOException e){
                                System.out.println("�������Ѿ��رգ�3����Զ��رտͻ���");
                                //��ǰ�߳���ͣ3���رգ�
                                try{
                                    Thread thread = Thread.currentThread();
                                    thread.sleep(3000);//��ͣ3���������ִ��
                                    System.exit(0);
                                }catch (InterruptedException  ex) {}
                            }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    System.out.println("�벻Ҫֻ����ո�");   
                }
            }  
        }
    }
    
    //��ѯ������ʷ��Ϣ��¼
    void history(String[] msgArray){
        //��historyArrayList�е�����Ԫ��ת��Ϊ�ַ�������
        //String[] historyArray = (String[])historyArrayList.toArray();         //����ת���Ǵ��
        Object[] array = historyArrayList.toArray();
        //������鳤�ȣ�����¼����Ϣ����
        int size = array.length;
        String[] historyArray = new String[size];
        for(int i = 0;i < size;i++)
            historyArray[i] = (String)array[i];

        //msgArray����Ϊ1����Ϊ����������"/history"���Ĭ����ʾ���50����Ϣ��û��50������ʾȫ����Ϣ��
        if(msgArray.length == 1){
            //û����Ϣ
            if(size == 0)
                System.out.println("��ǰû����ʷ��Ϣ��¼");
            //û��50������ʾȫ����Ϣ
            else if(size <= 50 && size > 0){
                for(int i = 0;i < size;i++)
                    System.out.println("#" + (i + 1) + " " + historyArray[i]);
            }
            //��Ϣ������50������ֻ��ʾ�����50����Ϣ
            else{
                for(int i = size - 50;i < size;i++)
                    System.out.println("#" + (i + 51 - size) + " " + historyArray[i]); 
            }
        }

        //msgArray���ȵ���3����Ϊ�Ϸ��Ĵ�������"/history"����
        else if(msgArray.length == 3){
            //���Խ���2�͵�3������ת��Ϊ������ת��ʧ�����׳�NumberFormatException�쳣
            try{
                int start_index = Integer.parseInt(msgArray[1]);
                int max_count   = Integer.parseInt(msgArray[2]);
                //�ж������ǲ���������
                if(start_index < 1 || max_count < 1)
                    System.out.println("/history�����������������Ϊ��������");
                //������������
                else{
                    //�鿴�����λ�ñ�������Ϣ��¼�������򷵻ز�ѯ��������ʾ
                    if(start_index > size)
                        System.out.println("������Ϣֻ��"+size+"����������ѡ��start_index");
                    //�鿴�����λ����������Ϣ��¼��
                    else{
                        //Ҫ��ѯ����ʼλ�ó�����Ϣ��¼����ʼλ�õģ��ʹ���Ϣ��¼����ʼλ�ò�ѯ
                        if((start_index - max_count) < 0)
                            for(int i = 0;i < start_index;i++)
                                System.out.println("#" + (i + 1) + " " + historyArray[i]);
                        //Ҫ��ѯ����ʼλ������Ϣ��¼���У��ʹ�Ҫ��ѯ����ʼλ�ÿ�ʼ��ѯ
                        else
                            for(int i = start_index - max_count;i < start_index;i++)
                                System.out.println("#" + (i + max_count - start_index + 1) + " " + historyArray[i]); 
                    }  
                }
            }catch(NumberFormatException e){
                System.out.println("/history�����������������Ϊ��������");
            }   
        }
        
        //�������ȵ�msgArray���ǲ��Ϸ����������
        else
            System.out.println("����Ϸ����Ϸ��������ʽΪ��/history [start_index max_count]");    
    }
    
    //˽�е��ڲ��ֻ࣬���ٱ�ChatroomClient����ʹ��
    //ͨ��ʵ��Runable�ӿ���ʵ�ֶ��߳����
    private class clientRunnable implements Runnable{
        //�߳��壬ʵ��Runnable�ӿ�
        public void run(){
            try{
                while(true){
                    int readyChannels = selector.select();                      //����ֵΪ������ͨ����
                    if(readyChannels == 0) continue;                            //û��ͨ�������ͽ�������
                    Set selectedKeys = selector.selectedKeys();                 //������ͨ���ļ��ŵ�һ��Set������������
                    Iterator iterator = selectedKeys.iterator();                //Set��ķ���iterator()����һ��Iterator����
                                                                                //ͨ��Iterator��������������Set������
                    //û�б�����Ļ�
                    while(iterator.hasNext()){
                        //�������õ��Ķ���ǿ��ת��ΪSelectionKey����
                        SelectionKey selectionKey = (SelectionKey)iterator.next();      
                                                                                //ÿ��ֻ����һ������
                        iterator.remove();                                      //���ʹ��Ķ�������
                        //��Ч����ʱ����
                        if(!selectionKey.isValid())
                            continue;   
                        //��������Ƕ����󣨴�channel�����ݣ�
                        else if(selectionKey.isReadable())
                            readFromChannel(selectionKey);
                        else break;
                    }
                }
            }catch (IOException io){}
        }
    }
    
    void readFromChannel(SelectionKey selectionKey) throws IOException{
        //��ȡselectionKey������channel����ǿ��ת��ΪSocketChannel����
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);                       //����512�ֽڵ��ֽڻ�����
        byteBuffer.clear();                                                     //������ջ�����
        String message = "";                            
        //��socketChannel�е����ݣ��ͻ�����charsetָ���ı��뷨���������ַ���������byteBuffer��
        //���ض���socketChannel�е��ֽ���
        //int bufferlength = socketChannel.read(byteBuffer);
        
        //��Ҫ����������int bufferlength = socketChannel.read(byteBuffer);����while(bufferlength > 0){}����ȡͨ���е���Ϣ
        //��ΪbyteBuffer������ʱû�õ���Ϣ�������������©��ͨ���е���Ϣ
        //���Ա����ȡ����ķ�������ȡ�������ж�ȡ��û�еĲ���ͬʱ��Ϊѭ����������һ������©��ͨ���е���Ϣ
        while(socketChannel.read(byteBuffer) > 0){
            byteBuffer.flip();                                                  //byteBufferת�䵽��״̬�����ɴ�byteBuffer�ж�����
            //��byteBuffer�е����ݣ���charsetָ���ı��뷨���������ַ�������ͬһ���뷽�����룬��ӵ�message��
            message += charset.decode(byteBuffer);
        } 
        //�ڿͻ�����ʾ��������������Ϣ
        System.out.println(message);
        historyArrayList.add(message);
        //���˶�Ӧ��channel����Ϊ׼�����������ͻ�������
        selectionKey.interestOps(SelectionKey.OP_READ);
    }
    
    public static void main(String[] args) throws IOException
    {
        new ChatroomClient().initClient();
    }
    
}