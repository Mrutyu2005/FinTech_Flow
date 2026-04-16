import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Header() {
  const { user } = useAuth();
  
  return (
    <header className="global-header">
      <div className="header-brand">Modern Banking</div>
      <div className="header-profile">
        <span>Welcome, {user || 'Guest'}</span>
      </div>
    </header>
  );
}
