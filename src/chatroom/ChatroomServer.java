package chatroom;

//�����ڱ�̹������Զ����ϵ�                                                              
import java.io.IOException;                                                     //��׼IO��
import java.net.InetSocketAddress;                                              //InetSocketAddress()��������
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
//import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author zhangyu
 */
public class ChatroomServer {
    
    Selector selector                           = null;                         //ѡ������ȫ�ֵ�
    private final int port                      = 12345;                        //�������˵Ķ˿�
    private final Charset charset               = Charset.forName("UTF-8");     //ͳһ����UTF-8(Unicode)����ͽ��룬�����������
    //HashSet�ఴ�չ�ϣ�㷨����ȡ�����еĶ��󣬴�ȡ�ٶȱȽϿ죬������ַ�������
    //userHashSet�洢�ͻ��˵��׽��֡���¼״̬�Լ��û�������¼����У�
    private HashSet userHashSet                 = new HashSet();
    //������ʵ�ֵ�List�������Ԫ�ؽ��п���������ʣ����String���͵���ʷ��Ϣ
    //�����ɷ�����ά��ÿ���ͻ��˵���Ϣ��¼�Ƚ��鷳���Ͱ������Ϣ��¼��ά�������ͻ���
    //private ArrayList<String> historyArrayList  = new ArrayList<String>();
    
    //��ʼ��������
    public void initServer() throws IOException{ 
        selector = Selector.open();                                             //�½�ѡ�������󣬿��Ƹ���channel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();   //�½���������ͨ���������½�����TCP����
                                                                                //�����ڱ�׼IO�е�ServerSocket
        serverSocketChannel.bind(new InetSocketAddress(port));                  //�������˶˿������ձ���portָ��
        serverSocketChannel.configureBlocking(false);                           //��Selector��ͬ����ʱ��������Ϊ����������ģʽ        
        //��serverSocketChannelע�ᵽselector��ע���¼�Ϊ������������
        //����һ��SelectionKey��Ķ�������û�б�Ҫʹ��������󣬹�û�ж�������
        //���������ϵ��serverSocketChannel��selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("ChatroomSever is working..." + new Date());
        judge(serverSocketChannel);    
    }
    
    //�ж��������Ͳ�����ش���
    void judge(ServerSocketChannel serverSocketChannel) throws IOException{
        while(true){                                                            //��ʼ����           
            //���ܸ�������Ȼ���ж���������Ͳ�������Ӧ����
            int readyChannels = selector.select();                              //����ֵΪ������ͨ����
            if(readyChannels == 0) continue;                                    //û��ͨ�������ͽ�������
            Set selectedKeys = selector.selectedKeys();                         //������ͨ���ļ��ŵ�һ��Set������������
            Iterator iterator = selectedKeys.iterator();                        //Set��ķ���iterator()����һ��Iterator����
                                                                                //ͨ��Iterator��������������Set������
            //û�б�����Ļ�
            while(iterator.hasNext()){
                SelectionKey selectionKey = (SelectionKey)iterator.next();      //�������õ��Ķ���ǿ��ת��ΪSelectionKey����
                                                                                //ÿ��ֻ����һ������
                iterator.remove();                                              //���ʹ��Ķ�������
                //��Ч����ʱ����
                if(!selectionKey.isValid())
                    continue;                           
                //�����������������
                else if(selectionKey.isAcceptable())
                    accept(serverSocketChannel,selectionKey);
                //��������Ƕ����󣨴�channel�����ݣ�
                else if(selectionKey.isReadable())
                    readFromChannel(selectionKey);
                else break;
            }
        }
    }
    
    //�������Կͻ��˵���������
    void accept(ServerSocketChannel serverSocketChannel,SelectionKey selectionKey) throws IOException{
        //��ȡselectionKey������channel����ǿ��ת��ΪServerSocketChannel����
        //ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
        //�½�һ��SocketChannel��������Ϊ������ģʽ��ע�ᵽselector�ϣ�ע���¼�Ϊ���¼�
        //��������socketChannel�������ʹ���������Ŀͻ�������
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);                 //����SelectionKey��Ķ���
        //���˶�Ӧ��channel����Ϊ׼�����������ͻ�������
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("Establish a connection with client "+socketChannel.getRemoteAddress()); 
        //��socketChannel��д����ʾ��¼����Ϣ����charsetָ���ı��뷨�����룩
        //�����ͻ��˾ͻ��յ�����������ַ���
        socketChannel.write(charset.encode("Please login"));
        //���볤��Ϊ3���ַ������飬��ŵ�ǰ�ͻ��˵��׽��֡���¼״̬�Լ��û�������¼����У������ӵ�userHashSet����
        String[] state = new String[3];
        state[0] = socketChannel.getRemoteAddress().toString();                 //��ǰ�ͻ����׽���
        state[1] = "0";                                                         //��ǰ�ͻ��˵�¼״̬
        state[2] = "";                                                          //��ǰ�ͻ����û���
        userHashSet.add(state);           
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
        
        //����������ӿͻ��ˣ��������ѵ�¼��Ҳ������δ��¼�ģ�ǿ���˳�������ɫ��stop��ť��
        //��ʹ��socketChannel.read(byteBuffer)�ͻ��׳�java.io.IOException
        //����������Ҫ�����쳣����
        try{
            //��Ҫ����������int bufferlength = socketChannel.read(byteBuffer);����while(bufferlength > 0){}����ȡͨ���е���Ϣ
            //��ΪbyteBuffer������ʱû�õ���Ϣ�������������©��ͨ���е���Ϣ
            //���Ա����ȡ����ķ�������ȡ�������ж�ȡ��û�еĲ���ͬʱ��Ϊѭ����������һ������©��ͨ���е���Ϣ
            while(socketChannel.read(byteBuffer) > 0){
                byteBuffer.flip();                                                  //byteBufferת�䵽��״̬�����ɴ�byteBuffer�ж�����
                //��byteBuffer�е����ݣ���charsetָ���ı��뷨���������ַ�������ͬһ���뷽�����룬��ӵ�message��
                message += charset.decode(byteBuffer);
            } 
            System.out.println("message from"+socketChannel.getRemoteAddress()+":"+message);
            //���˶�Ӧ��channel����Ϊ׼����һ�ν�������
            selectionKey.interestOps(SelectionKey.OP_READ);
            dealwithMessage(selectionKey,socketChannel,message);
        }catch(IOException e){
            abnormalQuit(selectionKey,socketChannel);  
        }
    }
    
    //�������˽��յ���Ϣ�������Ϣʶ��ʹ����������ĺ��Ĺ���
    void dealwithMessage(SelectionKey selectionKey,SocketChannel socketChannel,String message) throws IOException{
        //�ͻ���δ��¼
        if(!isLogin(socketChannel)){
            if(message.length() > 0){
                //��message�Կո�ָ��ٴ浽����msgArray��
                String[] msgArray = message.split(" ");
                switch(msgArray[0]){
                    case "/login"   : login(socketChannel,msgArray)         ;break;
                    case "/quit"    : logoutQuit(selectionKey,socketChannel);break; 
                    //δ��¼״̬������������ϢҪ��ͻ��˱���
                    default         : socketChannel.write(charset.encode("Ivalid command"));
                }                      
            }
//            //�Լӹ��ܣ����Ϳ���Ϣ��ֱ�ӻس������ɿո񣩵Ļ�����������ʾ�㲻Ҫ�ٷ���
//            //�������ڿͻ��������ò���������Ϣ����������ж�Ҳûʲô���ˣ���������
//            else
//                socketChannel.write(charset.encode("Do not send a blank message!"));
        }
        //�ͻ����ѵ�¼
        else{
            if(message.length() > 0){
                //���ڿͻ����Ѿ�������ֻ���Ϳո���
                //���������Կո�ָ���Ϣ�ٴ浽����msgArray�к�
                //����msgArray�϶���Ԫ�أ�����msgArray[0]�Ͳ�����������±�Խ���쳣ArrayIndexOutOfBoundsException
                //����Ҳ�Ͳ������쳣������
                String[] msgArray = message.split(" ");
                switch(msgArray[0]){
                    case "/quit"    : loginQuit(selectionKey,socketChannel);break;
                    case "//smile"  : smile(socketChannel);                 break;
                        
                    //�Լӵ�һЩԤ����Ϣ���������־���
                    case "//angry"  : angry(socketChannel);                 break;
                    case "//sad"    : sad(socketChannel);                   break;
                    case "//shock"  : shock(socketChannel);                 break;
                        
                    case "//hi"     : hi(socketChannel,msgArray);           break;
                    case "/to"      : to(socketChannel,msgArray);           break;
                    case "/who"     : who(socketChannel);                   break;
                    //��ʷ��Ϣ��ѯ�����ͻ�����
                    //case "/history" : history(msgArray);break;
                    //�����������Ϊ�㲥��Ϣ
                    default         :
                        socketChannel.write(charset.encode("��˵��"+message));
                        String yourName = getUserName(socketChannel);
                        broadCastExcept(socketChannel,yourName+"˵��"+message);
                        break;
                } 
            }
//            //�Լӹ��ܣ����Ϳ���Ϣ��ֱ�ӻس������ɿո񣩵Ļ�����������ʾ�㲻Ҫ�ٷ���
//            //�������ڿͻ��������ò���������Ϣ����������ж�Ҳûʲô���ˣ���������
//            else
//                socketChannel.write(charset.encode("Do not send a blank message!"));
        }
    }
    
    //�����ӿͻ��ˣ��������ѵ�¼��Ҳ������δ��¼�ģ�ǿ���˳��Ĵ�����
    void abnormalQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{
        //�ͻ���δ��¼
        if(!isLogin(socketChannel)){
            //����δ��¼״̬�µ��˳����������ر���������
            logoutQuit(selectionKey,socketChannel);
        }
        //�ͻ����ѵ�¼
        else{
            //�����ѵ�¼״̬�µ��˳����������㲥���������û����ر���������
            loginQuit(selectionKey,socketChannel);
        }
    }

    //����userHashSet���û���Ϣ���жϵ�ǰ�ͻ����Ƿ��Ѿ���¼
    //���socketChannel���Դ�ſͻ��˵ĵ�¼��Ϣ����ֱ�Ӷ��������¼��Ϣ����
    //��������socketChannel��û�д�ſͻ��˵ĵ�¼��Ϣ������ֻ�ò�������ѭ����ѯ�ķ�������ѯ�ͻ����Ƿ��Ѿ���¼
    boolean isLogin(SocketChannel socketChannel) throws IOException{
        boolean isLogin = false;
        //�ɵ�����iterator����userHashSet
        Iterator iterator = userHashSet.iterator();
        while(iterator.hasNext()){
            String[] state = (String[])iterator.next();                         //ǿ��ת��Ϊ�ַ�������
            //state[0]�д�ŵ��ǿͻ��˵��׽�����Ϣ�������ѯ��ǰ�ͻ��˶�Ӧ���û���Ϣ��¼
            if(!state[0].equals(socketChannel.getRemoteAddress().toString()))
                continue;
            //state[1]�д�ſͻ��˵ĵ�¼��Ϣ����"0"��һ����Ϊ"1"����ʾ�ѵ�¼
            if(!state[1].equals("0"))
                isLogin = true;                                                
        }
        return isLogin;
    }
    
    //����ͻ��˵�¼����
    void login(SocketChannel socketChannel,String[] msgArray) throws IOException{
        boolean nameExist     = false; 
        String userName       ="";

        //����ͻ����ύ���û���Ϊ�գ��򲻻���msgArray[1]
        //���׳������±�Խ���쳣ArrayIndexOutOfBoundsException����Ҫ���쳣����
        try{
            userName          = msgArray[1];                                    //��ǰ�ͻ����ύ���û���
        }catch(ArrayIndexOutOfBoundsException e){
            socketChannel.write(charset.encode("Username can not be empty!"));
        }
        
        //��ѯ����ǰ�ͻ��˵���Ϣ������currentState��
        String[] currentState = new String[3];
        //�û����ǿղźϷ�
        if(!userName.equals("")){
            Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //ǿ��ת��Ϊ�ַ�������
                //ȡ�ö�Ӧ��ǰ�ͻ��˵���Ϣ��¼����������û�����Ϣʱ�ٲ�ѯһ��
                //���Ǹ����ø�ֵ��ͨ���ı�currentState���ɸı�state�Ӷ��ı�userHashSet�еĶ�Ӧ��¼
                //��ѯ����ǰ�ͻ��ˣ���userHashSet�ж�Ӧ��¼�϶�û���û���������continueֱ�Ӳ�ѯ��һ����¼
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    currentState = state;
                    continue;
                }                   
                //state[2]�д�ŵ��ǿͻ��˵��û����������ѯ��ǰ�û����ǲ����Ѿ�����
                //���û������ڣ������ñ�־nameExistλtrue��������ѭ��
                if(state[2].equals(userName)){
                    nameExist = true;
                    break;
                }                                       
            }
            //��������û��ͬ���û������û�����¼��������������Ӧ��Ϣ���ͻ���
            if(!nameExist){
                currentState[1] = "1";                                          //��¼�û�Ϊ�ѵ�¼
                currentState[2] = userName;
                //������ǰ�û�
                socketChannel.write(charset.encode("You have logined"));
                //��������ǰ�û�������������û�
                broadCastExcept(socketChannel,userName+" has logined");
            }
            //����������ͬ���û�,��֪ͨ��ǰ�ͻ���
            else
                socketChannel.write(charset.encode("Name exist, please choose anthoer name."));                
        }
//        //�Լӹ��ܣ��û���������Ϊ��
//        //�������ڿͻ��������ò������յ��û�������������ж�Ҳûʲô����
//        else
//            socketChannel.write(charset.encode("Do not send a blank message!"));        
    }
    
    //δ��¼״̬��ֱ���˳�
    void logoutQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{ 
        System.out.println("ĳ�������ӵ�δ��¼�û��Ͽ�������");
        //�ر�����
        selectionKey.cancel();
        socketChannel.close();
        socketChannel.socket().close();
    }
    
    //��¼״̬�µ��˳�
    void loginQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{
        String userName = "";       
        //�ر�����ǰ�����userHashSet�еĿͻ�����Ϣ
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //ǿ��ת��Ϊ�ַ�������
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    userName = state[2];
                    //�Ƴ�userHashSet���˳��Ŀͻ�����Ϣ��¼
                    iterator.remove();
                    break;
                }
            }
        System.out.println(userName + " has quit.");
        broadCastExcept(socketChannel,userName+" has quit.");                   //�㲥�˳�����Ϣ
        //�ر�����
        selectionKey.cancel();
        socketChannel.close();
        socketChannel.socket().close();   
    }
    
    //Ԥ����Ϣ"//smile"�Ĵ���
    void smile(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"���Ϸ�����а��Ц��");
            socketChannel.write(charset.encode("�����Ϸ�����а��Ц��"));
    }
    
    //Ԥ����Ϣ"//angry"�Ĵ���
    void angry(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"�����������������");
            socketChannel.write(charset.encode("������������������"));
    }
    
    //Ԥ����Ϣ"//sad"�Ĵ���
    void sad(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"��ʾ�����Ǹ����˵Ĺ���");
            socketChannel.write(charset.encode("���ʾ�����Ǹ����˵Ĺ���"));
    }
    
    //Ԥ����Ϣ"//shock"�Ĵ���
    void shock(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"��ȫ������");
            socketChannel.write(charset.encode("����ȫ������"));
    }
    
    //Ԥ����Ϣ"//hi"�Ĵ���
    void hi(SocketChannel socketChannel,String[] msgArray) throws IOException{
        String yourName = getUserName(socketChannel);
        //"//hi"����û�в���ʱ����msgArray����Ϊ1
        if(msgArray.length == 1){
            broadCastExcept(socketChannel,yourName+"���Ҵ��к�����Hi����Һã�������~��");
            socketChannel.write(charset.encode("�����Ҵ��к�����Hi����Һã�������~��"));
        }
        else if(msgArray.length > 1){
            //"//hi"��������ŵ��ַ������������û����û���
            String userName = msgArray[1];
            broadCastExcept(socketChannel,yourName+"��"+userName+"���к�����Hi����ð�~��");
            socketChannel.write(charset.encode("����"+userName+"���к�����Hi����ð�~��"));
        }
    }
    
    //˽��ָ��Ĵ���
    void to(SocketChannel socketChannel,String[] msgArray) throws IOException{
        String yourName          = getUserName(socketChannel);                  //��õ�ǰ�ͻ����û���
        String userRemoteAddress = "";                                          //���Ŀ��ͻ����׽���
        String message           = "";                                          //��������Ϣ
        boolean userNameExist    = false;                                       //�ж��û����Ƿ����
        //���ָ��"/to"����ʲôҲû���ˣ�����ʾ�û�ָ��˽���û�
        if(msgArray.length == 1)
            socketChannel.write(charset.encode("����/to����ָ��˽���û���"));
        else if(msgArray.length > 1){
            //"/to"��������ŵ��ַ������������û����û���
            String userName = msgArray[1];           
            //���user_name���û��Լ�����ʾ�û���Ҫ���Լ��Ի�
            if(userName.equals(yourName))
                socketChannel.write(charset.encode("Stop talking to yourself!"));
            //���user_name�����û��Լ�
            else{
                Iterator iterator = userHashSet.iterator();
                while(iterator.hasNext()){
                    String[] state = (String[])iterator.next();                 //ǿ��ת��Ϊ�ַ�������
                    //�������userHashSet���ҵ�ָ���û������¼����û����׽���
                    if(state[2].equals(userName)){                              //state[2]�����ַ�����ʽ�Ŀͻ����û���
                        userRemoteAddress = state[0];                           //state[0]�����ַ�����ʽ�Ŀͻ����׽���
                        userNameExist = true;
                        break;
                    }
                }
                //�����userHashSet���Ҳ���ָ���û�������ʾ��user_name is not online.
                if(!userNameExist)
                    socketChannel.write(charset.encode(userName+" is not online."));
                //��userHashSet���ҵ�ָ���û�
                else{ 
                    //foreach������ʽ��for(Ԫ������t Ԫ�ر���x : ��������obj)���ʺ��ڱ���
                    //��selectionKey���ҵ���Ŀ���׽�������Ӧ��socketChannel
                    for(SelectionKey selectionKey : selector.keys()){            
                        Channel targetChannel = selectionKey.channel();
                        if(targetChannel instanceof SocketChannel){
                            SocketChannel otherChannel = (SocketChannel)targetChannel;
                            //��ÿͻ��˵��׽���
                            String address = otherChannel.getRemoteAddress().toString();
                            //�ͻ����׽�����Ŀ���׽�����ͬ�����ҵ�Ŀ��
                            if(address.equals(userRemoteAddress)){
                                //messageΪmsgArray��ȥǰ����Ԫ�أ�����������û���������������ַ���
                                for(int i = 2;i < msgArray.length;i++)
                                    message += msgArray[i];                               
                                socketChannel.write(charset.encode("���"+userName+"˵��"+message));
                                otherChannel.write(charset.encode(yourName+"����˵��"+message));
                                break;                                          //�ҵ�ָ���û���Ͳ���������
                            }                        
                        }
                    }
                }               
            }           
        }
    }
    
    //��ѯָ��Ĵ���
    void who(SocketChannel socketChannel) throws IOException{
        int userNum = 0;
        //��ͻ���д����ʾ��Ϣ������
        socketChannel.write(charset.encode("��ǰ�����û�Ϊ��\n"));
        //����userHashSet
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){               
                String[] state = (String[])iterator.next();                     //ǿ��ת��Ϊ�ַ�������
                //state[2]��ŵ����û��������ǿգ���˵���ǵ�¼�û�����Ҫ��ʾ
                if(!state[2].equals("")){
                    socketChannel.write(charset.encode(state[2]+"\n"));         //�г��û�����һ��һ��
                    userNum++;
                }                   
            }
        //��������֮���ڿͻ�����ʾ����������
        socketChannel.write(charset.encode("Total online user: "+userNum));    
    }
    

//    void history(String[] msgArray){
//        
//    }
   
    //���socketChannel��Ӧ���û���
   String getUserName(SocketChannel socketChannel) throws IOException{
       //��ȡsocketChannel��Ӧ�ͻ��˵��û���
        String userName = "";
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //ǿ��ת��Ϊ�ַ�������
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    userName = state[2];
                    break;
                }
            }
        return userName;
   }
    
    //��message�㲥����oneself��������Ѿ���¼��SocketChannel
    void broadCastExcept(SocketChannel oneself,String message) throws IOException{
        //foreach������ʽ��for(Ԫ������t Ԫ�ر���x : ��������obj)���ʺ��ڱ���
        //����selector�е�SelectionKey����
        for(SelectionKey selectionKey : selector.keys()){            
            Channel targetChannel = selectionKey.channel();
            //Ŀ��ͻ���Ϊ���˱�������������пͻ���
            if(targetChannel instanceof SocketChannel && targetChannel != oneself){
                SocketChannel otherChannel = (SocketChannel)targetChannel;
                //ֻ���ѵ�¼�û��㲥��Ϣ
                if(isLogin(otherChannel))
                    otherChannel.write(charset.encode(message));
            }
        }
    }
    
    public static void main(String[] args) throws IOException{
        new ChatroomServer().initServer();
    }
    
}