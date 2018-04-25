package wenzhe.aop;
/*
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;


@Aspect
public class MyAop {
  
  //@Pointcut(value = "execution (* wenzhe.umlgen.RecursableFileFilter.filter(..))")// && args(files)")
  //not work: @Pointcut(value = "execution (* wenzhe.umlgen.RecursableFileFilter.filter(..)) && args(files)", argNames = "files2")
  //@Pointcut(value = "execution (* wenzhe.umlgen.RecursableFileFilter.filter(..)) && args(files)", argNames = "files")
  //public void receiveMessagePointcut(Object msg) {}

  //@Before(value = "receiveMessagePointcut(msg)", argNames = "jp,msg")
//  @Before(value = "execution (* wenzhe.umlgen.*.*(..)) && args(files)", argNames = "files")
//  public void message(JoinPoint jp, Object files) {
//    Signature signature = jp.getSignature();
//    System.out.println(signature.getDeclaringTypeName() + "::" + signature.getName());
//  }

  @Before("execution(* wenzhe.umlgen.*.*(..))")
  public void message(JoinPoint jp) {
    Path dir = Paths.get(System.getProperty("user.home"), ".code2uml");
    Path seqFile = dir.resolve("sequence");
    if (!Files.exists(seqFile)) {
      return;
    }
    String recordRoot = "";
    try (BufferedReader br = Files.newBufferedReader(seqFile)) {
      recordRoot = br.readLine();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (recordRoot.isEmpty()) {
      return;
    }
    // get process id:
    String mxName = ManagementFactory.getRuntimeMXBean().getName();
    if (mxName == null || mxName.isEmpty()) {
      return;
    }
    String processId = mxName.split("@")[0];
    if (processId.isEmpty()) {
      return;
    }
    long threadId = Thread.currentThread().getId();
    Path targetPath = dir.resolve(recordRoot).resolve(processId).resolve(Long.toString(threadId));
    if (!Files.exists(targetPath)) {
      try {
        Files.createDirectories(targetPath.getParent());
        Files.createFile(targetPath);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    try (BufferedWriter bw = Files.newBufferedWriter(targetPath, StandardOpenOption.APPEND)) {
      Signature signature = jp.getSignature();
      bw.write(signature.getDeclaringTypeName());
      bw.write(":");
      bw.write(signature.getName());
      bw.write("{\n");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
      //System.out.println("Inject classes method");
    
  }
  
  @After("execution(* wenzhe.umlgen.*.*(..))")
  public void afterMessage(JoinPoint jp) {
    Path dir = Paths.get(System.getProperty("user.home"), ".code2uml");
    Path seqFile = dir.resolve("sequence");
    if (!Files.exists(seqFile)) {
      return;
    }
    String recordRoot = "";
    try (BufferedReader br = Files.newBufferedReader(seqFile)) {
      recordRoot = br.readLine();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (recordRoot.isEmpty()) {
      return;
    }
    // get process id:
    String mxName = ManagementFactory.getRuntimeMXBean().getName();
    if (mxName == null || mxName.isEmpty()) {
      return;
    }
    String processId = mxName.split("@")[0];
    if (processId.isEmpty()) {
      return;
    }
    long threadId = Thread.currentThread().getId();
    Path targetPath = dir.resolve(recordRoot).resolve(processId).resolve(Long.toString(threadId));
    if (!Files.exists(targetPath)) {
      try {
        Files.createDirectories(targetPath.getParent());
        Files.createFile(targetPath);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    try (BufferedWriter bw = Files.newBufferedWriter(targetPath, StandardOpenOption.APPEND)) {
      Signature signature = jp.getSignature();
      bw.write("}");
      bw.write(signature.getDeclaringTypeName());
      bw.write(":");
      bw.write(signature.getName());
      bw.write("\n");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
*/