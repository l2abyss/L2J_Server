/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  KenM
 */
public abstract class L2Transformation implements Cloneable, Runnable
{
    private final int _id;
    private final int _graphicalId;
    private final double _collisionRadius;
    private final double _collisionHeight;
    private long _duration;
    
    public static final int TRANSFORM_ZARICHE = 301;
    public static final int TRANSFORM_AKAMANAH = 302;
    
    private L2PcInstance _player;
    private long _startTime;
    private ScheduledFuture<?> _future;
    
    /**
     * 
     * @param id Internal id that server will use to associate this transformation 
     * @param graphicalId Client visible transformation id
     * @param duration Transformation duration in seconds
     * @param collisionRadius Collision Radius of the player while transformed
     * @param collisionHeight  Collision Height of the player while transformed
     */
    public L2Transformation(int id, int graphicalId, int duration, double collisionRadius, double collisionHeight)
    {
        _id = id;
        _graphicalId = graphicalId;
        _collisionRadius = collisionRadius;
        _collisionHeight = collisionHeight;
        this.setDuration(duration * 1000);
    }
    
    /**
     * 
     * @param id Internal id(will be used also as client graphical id) that server will use to associate this transformation 
     * @param duration Transformation duration in seconds
     * @param collisionRadius Collision Radius of the player while transformed
     * @param collisionHeight  Collision Height of the player while transformed
     */
    public L2Transformation(int id, int duration, double collisionRadius, double collisionHeight)
    {
        this(id, id, duration, collisionRadius, collisionHeight);
    }
    
    /**
     * @return Returns the id.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Returns the graphicalId.
     */
    public int getGraphicalId()
    {
        return _graphicalId;
    }

    /**
     * @return Returns the collisionRadius.
     */
    public double getCollisionRadius()
    {
        return _collisionRadius;
    }

    /**
     * @return Returns the collisionHeight.
     */
    public double getCollisionHeight()
    {
        return _collisionHeight;
    }

    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration)
    {
        _duration = duration;
    }
    
    /**
     * @return Returns the total duration in miliseconds.
     */
    public long getDuration()
    {
        return _duration;
    }
    
    /**
     * @return The remaining transformed time in miliseconds. An zero or negative value if the transformation has already ended.
     */
    public long getRemainingTime()
    {
        return (getStartTime() + this.getDuration()) - System.currentTimeMillis();
    }

    // Scriptable Events
    public abstract void onTransform();
    
    public abstract void onUntransform();

    /**
     * @param player The player to set.
     */
    private void setPlayer(L2PcInstance player)
    {
        _player = player;
    }

    /**
     * @return Returns the player.
     */
    public L2PcInstance getPlayer()
    {
        return _player;
    }
    
    /**
     * @param startTime The startTime to set.
     */
    public void setStartTime(long startTime)
    {
        _startTime = startTime;
    }

    /**
     * @return Returns the startTime.
     */
    private long getStartTime()
    {
        return _startTime;
    }

    /**
     * @param future The future to set.
     */
    public void setFuture(ScheduledFuture<?> future)
    {
        _future = future;
    }

    /**
     * @return Returns the future.
     */
    public ScheduledFuture<?> getFuture()
    {
        return _future;
    }

    public void start()
    {
        this.setStartTime(System.currentTimeMillis());
        this.resume();
    }
    
    public void resume()
    {
        this.setFuture(ThreadPoolManager.getInstance().scheduleGeneral(this, this.getRemainingTime()));
        this.getPlayer().transform(this);
    }
    
    public void run()
    {
        this.stop();
    }
    
    public void stop()
    {
        if (this.getRemainingTime() > 0)
        {
            this.getPlayer().untransform();
        }
    }
    
    public L2Transformation createTransformationForPlayer(L2PcInstance player)
    {
        try
        {
            L2Transformation transformation = (L2Transformation) this.clone();
            transformation.setPlayer(player);
            return transformation;
        }
        catch (CloneNotSupportedException e)
        {
            // should never happen
            return null;
        }
    }
    
    // Override if necessary
    public void onLevelUp()
    {
    	return;
    }
}
