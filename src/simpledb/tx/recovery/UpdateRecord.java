package simpledb.tx.recovery;

import simpledb.server.SimpleDB;
import simpledb.buffer.*;
import simpledb.file.Block;
import simpledb.log.BasicLogRecord;

class UpdateRecord implements LogRecord {
   private int txnum;
   private Block blk, newBlk;

   /**
    * Creates a new UpdateRecord log record.
    * @param txnum the ID of the specified transaction
    * @param blk the block containing the value
    * @param new blk 
    */
   public UpdateRecord(int txnum, Block blk, Block newBlk) {
      this.txnum = txnum;
      this.blk = blk;
      this.newBlk = newBlk;
   }

   /**
    * Creates a log record by reading five other values from the log.
    * @param rec the basic log record
    */
   public UpdateRecord(BasicLogRecord rec) {
      txnum = rec.nextInt();
      String file = rec.nextString();
      int blknum = rec.nextInt();
      blk = new Block(file, blknum);
      String newFile = rec.nextString();
      int newBlknum = rec.nextInt();
      newBlk = new Block(newFile, newBlknum);
   }

   /**
    * Writes a UpdateRecord record to the log.
    * This log record contains the SETINT operator,
    * followed by the transaction id, the filename, number,
    * and offset of the modified block, and the previous
    * integer value at that offset.
    * @return the LSN of the last log value
    */
   public int writeToLog() {
      Object[] rec = new Object[] {UPDATE, txnum, blk.fileName(),
         blk.number(), newBlk.fileName(), newBlk.number()};
      return logMgr.append(rec);
   }

   public int op() {
      return UPDATE;
   }

   public int txNumber() {
      return txnum;
   }

   public String toString() {
      return "<UPDATE " + " transaction number " + txnum + " block " + blk + " new block " + newBlk + ">";
   }

   /**
    * Replaces the specified data value with the value saved in the log record.
    * The method pins a buffer to the specified block,
    * calls setInt to restore the saved value
    * (using a dummy LSN), and unpins the buffer.
    * @see simpledb.tx.recovery.LogRecord#undo(int)
    */
   public void undo(int txnum) {
      BufferMgr buffMgr = SimpleDB.bufferMgr();
      Buffer buff = buffMgr.pin(blk);
      buff.restoreBlock(newBlk);
      buffMgr.unpin(buff);
   }
}
