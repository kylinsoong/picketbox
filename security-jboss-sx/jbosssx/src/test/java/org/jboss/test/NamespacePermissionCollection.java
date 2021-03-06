/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** The PermissionCollection object for NamespacePermissions.

@author Scott.Stark@jboss.org
@version $Revision$
*/
public class NamespacePermissionCollection extends PermissionCollection
{ 
   private static final long serialVersionUID = 1L;
   private TreeMap<PermissionName, List<NamespacePermission>> namespacePerms = 
      new TreeMap<PermissionName, List<NamespacePermission>>();
   private TreeMap<PermissionName, PermissionName> namespaceKeys = 
      new TreeMap<PermissionName, PermissionName>(new PermissionName.NameLengthComparator());

    /** Creates new NamespacePermission */
    public NamespacePermissionCollection()
    {
    }

    public void add(Permission permission)
    {
        if( this.isReadOnly() )
            throw new SecurityException("Cannot add permission to read-only collection");
        if( (permission instanceof NamespacePermission) == false )
            throw new IllegalArgumentException("Only NamespacePermission can be added, invalid="+permission);
        NamespacePermission np = (NamespacePermission) permission;
        PermissionName key = np.getFullName();
        List<NamespacePermission> tmp = namespacePerms.get(key);
        if( tmp == null )
        {
            tmp = new ArrayList<NamespacePermission>();
            namespacePerms.put(key, tmp);
            namespaceKeys.put(key, key);
        }
        tmp.add(np);
    }

    /** Locate the closest permissions assigned to the namespace. This is based
     *on the viewing the permission name as a heirarchical PermissionName and
     */
    public boolean implies(Permission permission)
    {
        boolean implies = false;
        if( namespacePerms.isEmpty() == true )
            return false;

        NamespacePermission np = (NamespacePermission) permission;
        // See if there is an exact permission for the name
        PermissionName key = np.getFullName();
        List<NamespacePermission> tmp = namespacePerms.get(key);
        if( tmp == null )
        {   // Find the closest parent position.
            SortedMap<PermissionName, List<NamespacePermission>> headMap = namespacePerms.headMap(key);
            try
            {
                PermissionName lastKey = (PermissionName) headMap.lastKey();
                if( lastKey.isParent(key) == true )
                    tmp = namespacePerms.get(lastKey);
                else
                {
                    PermissionName[] keys = {};
                    keys = (PermissionName[]) headMap.keySet().toArray(keys);
                    for(int k = keys.length-1; k >= 0; k --)
                    {
                        lastKey = keys[k];
                        if( lastKey.isParent(key) == true )
                        {
                            tmp = namespacePerms.get(lastKey);
                            break;
                        }
                    }
                }
            }
            catch(NoSuchElementException e)
            {   /* Assign the first permission
                Object firstKey = namespacePerms.firstKey();
                tmp = (ArrayList) namespacePerms.get(firstKey);
		*/
            }
        }

        // See if the permission is implied by any we found
        if( tmp != null )
            implies = isImplied(tmp, np);
        //System.out.println("NPC["+this+"].implies("+np+") -> "+implies);
        return implies;
    }

    public Enumeration<Permission> elements()
    {
        Set<PermissionName> s = namespaceKeys.keySet();
        final Iterator<PermissionName> iter = s.iterator();
        Enumeration<Permission> elements = new Enumeration<Permission>()
        {
            List<NamespacePermission> activeEntry;
            int index;
            
            /*
             * (non-Javadoc)
             * @see java.util.Enumeration#hasMoreElements()
             */
            public boolean hasMoreElements()
            {
                boolean hasMoreElements = true;
                if( activeEntry == null || index >= activeEntry.size() )
                {
                    hasMoreElements = iter.hasNext();
                    activeEntry = null;
                }
                return hasMoreElements;
            }
            
            /*
             * (non-Javadoc)
             * @see java.util.Enumeration#nextElement()
             */
            public NamespacePermission nextElement()
            {
               NamespacePermission next = null;
                if( activeEntry == null )
                {
                    Object key = iter.next();
                    activeEntry = namespacePerms.get(key);
                    index = 0;
                    next = activeEntry.get(index ++);
                }
                else
                {
                    next = activeEntry.get(index ++);
                }
                return next;
            }
        };
        return elements;
    }


    private boolean isImplied(List<NamespacePermission> permissions, NamespacePermission np)
    {
        boolean isImplied = false;
        for(int p = 0; p < permissions.size(); p ++)
        {
            Permission perm = (Permission) permissions.get(p);
            isImplied |= perm.implies(np);
            if( isImplied == true )
                break;
        }
        return isImplied;
    }
}
