import { mount } from 'svelte';
import App from './App.svelte';
import { apply } from './lib/theme.svelte';
import './app.css';

apply();

export default mount(App, { target: document.body });
