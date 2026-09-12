import { WORLD_REGIONS } from './world-regions';
import aus from '../assets/flags/aus.png';
import br from '../assets/flags/br.png';
import ger from '../assets/flags/ger.png';
import jp from '../assets/flags/jp.png';
import sg from '../assets/flags/sg.png';
import uk from '../assets/flags/uk.png';
import us from '../assets/flags/us.png';
import za from '../assets/flags/za.png';

/*
 * The sprites in ../assets/flags are unmodified copies of the world hopper
 * flags from RuneLite, used under BSD-2-Clause. See public/NOTICE.txt, which
 * ships with the site and carries the required copyright notice.
 */

/** The regions the plugin ships a flag sprite for. */
export type RegionCode = 'aus' | 'br' | 'ger' | 'jp' | 'sg' | 'uk' | 'us' | 'za';

export interface Flag {
  src: string;
  country: string;
}

const FLAGS: Readonly<Record<RegionCode, Flag>> = {
  aus: { src: aus, country: 'Australia' },
  br: { src: br, country: 'Brazil' },
  ger: { src: ger, country: 'Germany' },
  jp: { src: jp, country: 'Japan' },
  sg: { src: sg, country: 'Singapore' },
  uk: { src: uk, country: 'United Kingdom' },
  us: { src: us, country: 'United States' },
  za: { src: za, country: 'South Africa' },
};

/**
 * Flag for a world, or undefined for one missing from the generated table —
 * a world added since the last `npm run worlds`.
 *
 * The plugin further splits US worlds into east and west coast sprites, but
 * that split comes from a game-cache enum the browser has no access to.
 */
export function flagFor(worldId: number): Flag | undefined {
  const region = WORLD_REGIONS[worldId];
  return region ? FLAGS[region] : undefined;
}
