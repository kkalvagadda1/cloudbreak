install-freeipa:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_install.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_install-executed
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - DOMAIN: {{salt['pillar.get']('freeipa:domain')}}
        - REALM: {{salt['pillar.get']('freeipa:realm')}}
    - failhard: True
    - unless: test -f /var/log/freeipa_install-executed
    - require:
        - file: /opt/salt/scripts/freeipa_install.sh
