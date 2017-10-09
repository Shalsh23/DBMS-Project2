package simpledb.buffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import simpledb.file.*;
//Code added by Ashwin
import static simpledb.server.Startup.gclock_iterator;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
public class BasicBufferMgr {
   private Buffer[] bufferpool;
   private int numAvailable;
   private int availableUnpinnedBuffers;
   private Map<Integer, Buffer> bufferMap;
   static Scanner s = new Scanner(System.in);
   private static int gclock_index=0;
   private static int bufferpoolsize=0;
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    * @param numbuffs the number of buffer slots to allocate
    */
   public BasicBufferMgr(int numbuffs) {
      bufferpool = new Buffer[numbuffs];
      bufferMap = new HashMap<Integer, Buffer>();
      
      numAvailable = numbuffs;
      bufferpoolsize=numbuffs;
      availableUnpinnedBuffers=numbuffs;
      for (int i=0; i<numbuffs; i++)
         bufferpool[i] = new Buffer();
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
      for (Buffer buff : bufferpool)
    	  if (buff.isModifiedBy(txnum))
         		buff.flush();
   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
	   //System.out.println("In PIN"); 
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         buff.assignToBlock(blk);
         //-------->
         bufferMap.put(blk.hashCode(), buff);
      }
      if (!buff.isPinned())    {
    	  //System.out.println("In PIN AGAIN");
         numAvailable--;
         availableUnpinnedBuffers--;
      }
      buff.pin();
      //Added by Ashwin
      buff.resetReferenceCounter();
      //System.out.println("Inserting into the map");
      BufferPrintStatus();
      PinPrintStatus();
      ReferenceCountStatus();
      UnpinnedBuffers();
      //pramod's code
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
       //System.out.println("Into pinNew()");
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      buff.assignToNew(filename, fmtr);
      numAvailable--;
      availableUnpinnedBuffers--;
      buff.pin();
      bufferMap.put(buff.block().hashCode(), buff);
      //Added by Ashwin
      buff.resetReferenceCounter();
      BufferPrintStatus();
      PinPrintStatus();
      ReferenceCountStatus();
      UnpinnedBuffers();
      Collection<Buffer> test=bufferMap.values();
      Iterator<Buffer> iter=test.iterator();
      while(iter.hasNext())
      {
          Buffer tempbuff=iter.next();
          System.out.println("Block : "+tempbuff.block().number());
      }
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
	   //System.out.println("In UNPIN"); 
      buff.unpin();
      if (!buff.isPinned()){
    	  //numAvailable++;
          availableUnpinnedBuffers++;
      }
          
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
      /*for (Buffer buff : bufferpool) {
         Block b = buff.block();
         if (b != null && b.equals(blk))
            return buff;
      }
      return null;*/
	   System.out.println("In Existing Buffer" + blk);
       return bufferMap.get(blk.hashCode());
   }
   //Code added by Ashwin
   //This currently just returns the 1st unpinned buffer. Need to make it 'intelligent'.
   private Buffer chooseUnpinnedBuffer() {
      Buffer unpinned=null;
      //System.out.println("chooseUnpinnedBuffer "+ numAvailable);
      //s.nextInt();
      //Bufferpool is not yet full
      //if(numAvailable > 0)
      //{
       for (Buffer buff : bufferpool)
      {
         //if (!buff.isPinned())
    	   if (buff.block() == null)
         {    unpinned=buff;
         	 return unpinned;
         }
      }
      {
         //numAvailable++;
          unpinned=GClock();
      }
      return unpinned;
   }
   
   //Code added by Ashwin
   private Buffer GClock()
   {
       //Point 1 : Iterate through the bufferpool i times
       //Point 2 : If any buffer is pinned, it won't be ever considered for replacement
       //Point 3 : As the clock head is spun across every unpinned buffer, decrease it's reference_counter
       //Point 4 : If any unpinned buffer having reference_counter=0 is encountered, break the loop and this is the buffer to be replaced
       int index=1;
       System.out.println("GClock policy is being used for buffer replacement");
       try {
            Thread.sleep(40);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
       //int victim_found=0;
       int poolsize=0;
       //Sanity check for the bufferpoolsize
       for(Buffer buff : bufferpool)
    	   poolsize++;
       /*if(poolsize==bufferpoolsize)
    	   	System.out.println("Buffer pool sizes match");
       else
    	   	System.out.println("Buffer pool sizes mismatch");*/
       
       //new code
       int iteration_controller=0;
       Buffer buff=null;
       iteration_controller=(gclock_iterator+1)*bufferpoolsize;
       for(index=0;index<iteration_controller;index++)    //Running this gclock_iterator times;
       {
    	   gclock_index=(gclock_index+1)%bufferpoolsize;
    	   System.out.println("gclock_index: "+gclock_index);
    	   buff=bufferpool[gclock_index];
               if(!buff.isPinned())                //If the buffer is unpinned
               {
                   buff.decrementReferenceCounter();
                   System.out.println("Decrementing");
                   ReferenceCountStatus();
                   if(buff.getReferenceCounter()==0){
                       System.out.println(buff.block()+" : This particular block is going to be replaced");
                       buff.flush();
                       bufferMap.remove(buff.block().hashCode());
                       return buff;
                   }
               }
       }
       
       
       //old code
       /*for(index=1;index<=gclock_iterator+1;index++)    //Running this gclock_iterator times;
       {
           for(Buffer buff : bufferpool)
           {
               //if(buff!=null)
               {
               if(!buff.isPinned())                //If the buffer is unpinned
               {
                   buff.decrementReferenceCounter();
                   System.out.println("Decrementing");
                   ReferenceCountStatus();
                   if(buff.getReferenceCounter()==0){
                       System.out.println(buff.block()+" : This particular block is going to be replaced");
                       buff.flush();
                       bufferMap.remove(buff.block().hashCode());
                       return buff;
                   }
               }
               }
           }
       }*/
       return null;
   }
   
   public void BufferPrintStatus()
   {
       System.out.println("Bufferpool contents");
       for(Buffer buff : bufferpool)
       {
           if(buff.block()==null)
        	   System.out.print(" - ");
           else
               System.out.print(buff.block()+" ");
       }
       System.out.println("");
   }
   
   public void PinPrintStatus()
   {
       System.out.println("Pin counts for buffers in the bufferpool");
       for(Buffer buff : bufferpool)
       {
           System.out.print(buff.getPinCount()+" ");
       }
       System.out.println("");
   }
   
   public void ReferenceCountStatus()
   {
       System.out.println("Reference counters for buffers in the bufferpool");
       for(Buffer buff : bufferpool)
       {
    	   if(buff.block()==null)
    		   System.out.print(" - ");
    	   else
    		   
    		   System.out.print(buff.getReferenceCounter()+" ");
       }
       System.out.println("");
   }
   public void UnpinnedBuffers()
   {
       System.out.println("Number of available buffers in the bufferpool : "+availableUnpinnedBuffers);
   }
   
}