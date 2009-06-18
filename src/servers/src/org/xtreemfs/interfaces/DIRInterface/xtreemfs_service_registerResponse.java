package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_registerResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1110;

    
    public xtreemfs_service_registerResponse() { returnValue = 0; }
    public xtreemfs_service_registerResponse( long returnValue ) { this.returnValue = returnValue; }
    public xtreemfs_service_registerResponse( Object from_hash_map ) { returnValue = 0; this.deserialize( from_hash_map ); }
    public xtreemfs_service_registerResponse( Object[] from_array ) { returnValue = 0;this.deserialize( from_array ); }

    public long getReturnValue() { return returnValue; }
    public void setReturnValue( long returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "xtreemfs_service_registerResponse( " + Long.toString( returnValue ) + " )";
    }

    // Serializable
    public int getTag() { return 1110; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.returnValue = ( ( Long )from_hash_map.get( "returnValue" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.returnValue = ( ( Long )from_array[0] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        returnValue = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "returnValue", new Long( returnValue ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putLong( returnValue );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }


    private long returnValue;    

}

